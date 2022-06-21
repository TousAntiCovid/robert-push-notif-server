package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
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
public class ApnsTemplate {

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsClientFactory apnsClientFactory;

    private final Semaphore semaphore;

    private final BlockingBucket rateLimitingBucket;

    public ApnsTemplate(
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

    public <T> void sendNotification(final NotificationHandler<T> handler) {
        this.sendNotification(handler, new ConcurrentLinkedQueue<>(apnsClientFactory.getApnsClients()));
    }

    private <T> void sendNotification(final NotificationHandler<T> notificationHandler,
            final Queue<ApnsClientDecorator> apnsClientsQueue) {
        try {
            rateLimitingBucket.consume(1);
            semaphore.acquire();
        } catch (InterruptedException e) {
            log.error("error during rate limiting process", e);
            return;
        }

        var apnsClient = apnsClientsQueue.poll();
        assert apnsClient != null;
        final var sendNotificationFuture = apnsClient.sendNotification(
                notificationHandler.buildNotification(robertPushServerProperties.getApns().getTopic())
        );

        sendNotificationFuture.whenComplete((response, cause) -> {
            semaphore.release();
            if (Objects.nonNull(response)) {
                if (response.isAccepted()) {
                    notificationHandler.onSuccess();
                } else {
                    final String rejectionReason = response.getRejectionReason();

                    if (isNotBlank(rejectionReason) && this.robertPushServerProperties.getApns()
                            .getInactiveRejectionReason().contains(rejectionReason)) {
                        // errors which means to try on another apn server
                        if (apnsClientsQueue.size() > 0) {
                            // try next apn client in the queue
                            this.sendNotification(notificationHandler, apnsClientsQueue);
                        } else {
                            // token was unsuccessful on every client, disable it
                            notificationHandler.onTokenNotFound(rejectionReason);
                        }
                    } else {
                        notificationHandler.onRejection(rejectionReason);
                    }
                }
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                log.warn("Push Notification sent by {} failed", apnsClient.getId(), cause);
                notificationHandler.onRejection(truncate(cause.getMessage(), 255));
            }
        }).exceptionally(e -> {
            log.error("Unexpected error occurred", e);
            return null;
        });
    }
}
