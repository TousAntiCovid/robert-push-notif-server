package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import io.github.bucket4j.*;
import io.github.bucket4j.local.LockFreeBucket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.eatthepath.pushy.apns.util.SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD;

@Slf4j
@Service
public class ApnsPushNotificationService {

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsClientFactory apnsClientFactory;

    private final Semaphore semaphore;

    private final BlockingBucket rateLimitingBucket;

    public ApnsPushNotificationService(
            RobertPushServerProperties robertPushServerProperties,
            ApnsClientFactory apnsClientFactory) {
        this.robertPushServerProperties = robertPushServerProperties;
        this.apnsClientFactory = apnsClientFactory;

        semaphore = new Semaphore(robertPushServerProperties.getMaxNumberOfOutstandingNotification());

        Bandwidth limit = Bandwidth.classic(
                robertPushServerProperties.getMaxNotificationsPerSecond(),
                Refill.intervally(
                        robertPushServerProperties.getMaxNotificationsPerSecond(),
                        Duration.ofSeconds(1)
                )
        );
        BucketConfiguration configuration = Bucket4j.configurationBuilder().addLimit(limit).build();
        this.rateLimitingBucket = new LockFreeBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public void waitUntilNoActivity(Duration toleranceDuration) {
        try {
            // first attempt
            isDetectedSomeActivity();
            // As the monitoring of the end of activity is not 100% efficient
            // e.g in case of latency during reading in database
            do {
                TimeUnit.SECONDS.sleep(TimeUnit.SECONDS.convert(toleranceDuration));
            } while (isDetectedSomeActivity());
        } catch (InterruptedException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private boolean isDetectedSomeActivity() throws InterruptedException {
        var count = 0;
        do {
            log.info(
                    "it remains {} active threads",
                    robertPushServerProperties.getMaxNumberOfOutstandingNotification()
                            - semaphore.availablePermits()
            );
            count++;
            TimeUnit.SECONDS.sleep(5);
        } while (semaphore.availablePermits() < robertPushServerProperties.getMaxNumberOfOutstandingNotification());
        return (count > 1);
    }

    private SimpleApnsPushNotification buildPushNotification(final String token) {

        final String payload = new SimpleApnsPayloadBuilder()
                .setContentAvailable(true)
                .setBadgeNumber(0)
                .build();

        return new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(token).toLowerCase(),
                this.robertPushServerProperties.getApns().getTopic(),
                payload,
                Instant.now().plus(DEFAULT_EXPIRATION_PERIOD),
                DeliveryPriority.IMMEDIATE,
                PushType.BACKGROUND
        );
    }

    public <T> void sendPushInfoNotification(String token, T data, Consumer<T> onSuccess,
            BiConsumer<T, String> onRejection, Consumer<T> disableToken) {
        this.sendNotification(
                token, data, new ConcurrentLinkedQueue<>(apnsClientFactory.getApnsClients()), onSuccess, onRejection,
                disableToken
        );
    }

    private <T> void sendNotification(String token,
            T data,
            Queue<ApnsClientDecorator> apnsClientsQueue,
            Consumer<T> onAccepted,
            BiConsumer<T, String> onRejection,
            Consumer<T> disableToken) {
        try {
            rateLimitingBucket.consume(1);
            semaphore.acquire();
            final SimpleApnsPushNotification pushNotification = buildPushNotification(token);
            final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;

            ApnsClientDecorator apnsClient = apnsClientsQueue.poll();
            assert apnsClient != null;
            sendNotificationFuture = apnsClient.sendNotification(pushNotification);

            sendNotificationFuture.whenComplete((response, cause) -> {
                semaphore.release();
                if (Objects.nonNull(response)) {
                    if (response.isAccepted()) {
                        log.debug(
                                "Push notification sent by {} accepted by APNs gateway for the token ({})",
                                apnsClient.getId(), token
                        );
                        onAccepted.accept(data);
                    } else {
                        log.debug(
                                "Push notification sent by {} rejected by the APNs gateway: {}",
                                apnsClient.getId(), response.getRejectionReason()
                        );
                        final String rejectionReason = response.getRejectionReason();

                        if (isRejectionReasonInSupportedReasons(rejectionReason)) {
                            if (apnsClientsQueue.size() > 0) {
                                this.sendNotification(
                                        token, data, apnsClientsQueue, onAccepted, onRejection, disableToken
                                );
                            } else {
                                disableToken.accept(data);
                                onRejection.accept(data, rejectionReason);
                            }
                        } else {
                            onRejection.accept(data, rejectionReason);
                        }

                        response.getTokenInvalidationTimestamp().ifPresent(
                                timestamp -> log.debug("\t…and the token is invalid as of {}", timestamp)
                        );
                    }
                } else {
                    // Something went wrong when trying to send the notification to the
                    // APNs server. Note that this is distinct from a rejection from
                    // the server, and indicates that something went wrong when actually
                    // sending the notification or waiting for a reply.
                    log.warn("Push Notification sent by {} failed", apnsClient.getId(), cause);
                    onRejection.accept(data, StringUtils.truncate(cause.getMessage(), 255));
                }
            }).exceptionally(e -> {
                log.error("Unexpected error occurred", e);
                return null;
            });
        } catch (InterruptedException e) {
            log.error("Unexpected error occurred", e);
        }
    }

    private boolean isRejectionReasonInSupportedReasons(String rejectionReason) {
        return StringUtils.isNotBlank(rejectionReason)
                && this.robertPushServerProperties.getApns().getInactiveRejectionReason()
                        .contains(rejectionReason);
    }
}
