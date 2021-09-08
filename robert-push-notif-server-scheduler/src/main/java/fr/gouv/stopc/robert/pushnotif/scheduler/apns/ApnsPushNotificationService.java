package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LockFreeBucket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class ApnsPushNotificationService {

    private final RobertPushServerProperties robertPushServerProperties;

    private final PushInfoDao pushInfoDao;

    private final ApnsClientFactory apnsClientFactory;

    private final Semaphore semaphore;

    private final BlockingBucket rateLimitingBucket;

    public ApnsPushNotificationService(
            RobertPushServerProperties robertPushServerProperties, PushInfoDao pushInfoDao,
            ApnsClientFactory apnsClientFactory) {
        this.robertPushServerProperties = robertPushServerProperties;
        this.pushInfoDao = pushInfoDao;
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

    public int getAvailablePermits() {
        return this.semaphore.availablePermits();
    }

    private SimpleApnsPushNotification buildPushNotification(PushInfo push) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setContentAvailable(true);
        payloadBuilder.setBadgeNumber(0);

        final String payload = payloadBuilder.build();
        final String token = TokenUtil.sanitizeTokenString(push.getToken());

        return new SimpleApnsPushNotification(
                token.toLowerCase(), this.robertPushServerProperties.getApns().getTopic(), payload,
                Instant.now().plus(SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD), DeliveryPriority.IMMEDIATE,
                PushType.BACKGROUND
        );

    }

    public void sendPushNotification(PushInfo push) {
        this.sendNotification(push, new ConcurrentLinkedQueue<>(apnsClientFactory.getApnsClients()));
    }

    private void sendNotification(PushInfo push, Queue<ApnsClientDecorator> apnsClientsQueue) {
        try {
            rateLimitingBucket.consume(1);
            semaphore.acquire();
            final SimpleApnsPushNotification pushNotification = buildPushNotification(push);
            final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;

            ApnsClientDecorator apnsClient = apnsClientsQueue.poll();
            sendNotificationFuture = apnsClient.sendNotification(pushNotification);

            sendNotificationFuture.whenComplete((response, cause) -> {
                semaphore.release();
                if (Objects.nonNull(response)) {
                    if (response.isAccepted()) {
                        log.debug(
                                "Push notification sent by {} accepted by APNs gateway for the token ({})",
                                apnsClient.getId(), push.getToken()
                        );
                        push.setLastSuccessfulPush(LocalDateTime.now());
                        push.setSuccessfulPushSent(push.getSuccessfulPushSent() + 1);
                        pushInfoDao.updateSuccessFulPushedNotif(push);
                    } else {
                        log.debug(
                                "Push notification sent by {} rejected by the APNs gateway: {}",
                                apnsClient.getId(), response.getRejectionReason()
                        );
                        final String rejectionReason = response.getRejectionReason();

                        if (StringUtils.isNotBlank(rejectionReason)
                                && this.robertPushServerProperties.getApns().getInactiveRejectionReason()
                                        .contains(rejectionReason)) {
                            if (apnsClientsQueue.size() > 0) {
                                this.sendNotification(push, apnsClientsQueue);
                            } else {
                                push.setActive(false);
                                push.setLastErrorCode(rejectionReason);
                                push.setLastFailurePush(LocalDateTime.now());
                                push.setFailedPushSent(push.getFailedPushSent() + 1);
                                pushInfoDao.updateFailurePushedNotif(push);
                            }
                        } else {
                            push.setLastErrorCode(rejectionReason);
                            push.setLastFailurePush(LocalDateTime.now());
                            push.setFailedPushSent(push.getFailedPushSent() + 1);
                            pushInfoDao.updateFailurePushedNotif(push);
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
                    log.warn("Push Notification sent by {} failed => {}", apnsClient.getId(), cause);
                    push.setLastErrorCode(StringUtils.truncate(cause.getMessage(), 255));
                    push.setLastFailurePush(LocalDateTime.now());
                    push.setFailedPushSent(push.getFailedPushSent() + 1);
                    pushInfoDao.updateFailurePushedNotif(push);
                }
            }).exceptionally(e -> {
                log.error("Unexpected error occurred", e);
                return null;
            });
        } catch (InterruptedException e) {
            log.error("Unexpected error occurred", e);
        }
    }
}
