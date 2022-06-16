package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import io.github.bucket4j.*;
import io.github.bucket4j.local.LockFreeBucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.truncate;

@Slf4j
@Service
public class ApnsPushNotificationService {

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsClientFactory apnsClientFactory;

    private final Semaphore semaphore;

    private final BlockingBucket rateLimitingBucket;

    public ApnsPushNotificationService(
            final RobertPushServerProperties robertPushServerProperties,
            final ApnsClientFactory apnsClientFactory) {
        this.robertPushServerProperties = robertPushServerProperties;
        this.apnsClientFactory = apnsClientFactory;

        semaphore = new Semaphore(robertPushServerProperties.getMaxNumberOfOutstandingNotification());

        final var limit = Bandwidth.classic(
                robertPushServerProperties.getMaxNotificationsPerSecond(),
                Refill.intervally(
                        robertPushServerProperties.getMaxNotificationsPerSecond(),
                        Duration.ofSeconds(1)
                )
        );
        final var configuration = Bucket4j.configurationBuilder().addLimit(limit).build();
        this.rateLimitingBucket = new LockFreeBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public void waitUntilNoActivity(Duration toleranceDuration) {
        try {
            // first attempt
            isDetectedSomeActivity();
            // As the monitoring of the end of activity is not 100% efficient
            // e.g in case of latency during reading in database
            do {
                SECONDS.sleep(SECONDS.convert(toleranceDuration));
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
                    robertPushServerProperties.getMaxNumberOfOutstandingNotification() - semaphore.availablePermits()
            );
            count++;
            SECONDS.sleep(5);
        } while (semaphore.availablePermits() < robertPushServerProperties.getMaxNumberOfOutstandingNotification());
        return (count > 1);
    }

    public <T> void sendNotification(final NotificationPilot<T> pilot) {
        this.sendNotification(pilot, new ConcurrentLinkedQueue<>(apnsClientFactory.getApnsClients()));
    }

    private <T> void sendNotification(final NotificationPilot<T> notificationPilot,
            final Queue<ApnsClientDecorator> apnsClientsQueue) {
        try {
            rateLimitingBucket.consume(1);
            semaphore.acquire();

            var apnsClient = apnsClientsQueue.poll();
            assert apnsClient != null;
            final var sendNotificationFuture = apnsClient.sendNotification(
                    notificationPilot.buildNotification(robertPushServerProperties.getApns().getTopic())
            );

            sendNotificationFuture.whenComplete((response, cause) -> {
                semaphore.release();
                if (Objects.nonNull(response)) {
                    if (response.isAccepted()) {
                        log.debug(
                                "Push notification sent by {} accepted by APNs gateway for the token ({})",
                                apnsClient.getId(), notificationPilot.getToken()
                        );
                        notificationPilot.updateOnNotificationSuccess();
                    } else {
                        log.debug(
                                "Push notification sent by {} rejected by the APNs gateway: {}",
                                apnsClient.getId(), response.getRejectionReason()
                        );
                        final String rejectionReason = response.getRejectionReason();

                        if (isNotBlank(rejectionReason) && this.robertPushServerProperties.getApns()
                                .getInactiveRejectionReason().contains(rejectionReason)) {
                            if (apnsClientsQueue.size() > 0) {
                                this.sendNotification(notificationPilot, apnsClientsQueue);
                            } else {
                                notificationPilot.disableToken();
                                notificationPilot.updateOnNotificationRejection(rejectionReason);
                            }
                        } else {
                            notificationPilot.updateOnNotificationRejection(rejectionReason);
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
                    notificationPilot.updateOnNotificationRejection(truncate(cause.getMessage(), 255));
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
