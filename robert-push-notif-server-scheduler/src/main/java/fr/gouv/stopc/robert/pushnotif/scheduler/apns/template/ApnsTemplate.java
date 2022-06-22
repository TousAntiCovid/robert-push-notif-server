package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.NotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.truncate;

/**
 * Used to identify the APN server host & port when an error occurs
 */
@Slf4j
public class ApnsTemplate implements ApnsOperations {

    private final ApnsClient apnsClient;

    private final String host;

    private final int port;

    private final RobertPushServerProperties robertPushServerProperties;

    private final Semaphore semaphore;

    public ApnsTemplate(final ApnsClient apnsClient,
            final String host,
            final int port,
            final RobertPushServerProperties robertPushServerProperties) {
        this.apnsClient = apnsClient;
        this.host = host;
        this.port = port;
        this.robertPushServerProperties = robertPushServerProperties;
        semaphore = new Semaphore(this.robertPushServerProperties.getMaxNumberOfOutstandingNotification());
    }

    public <T> void sendNotification(final NotificationHandler<T> notificationHandler) {
        try {
            semaphore.acquire(1);
        } catch (InterruptedException e) {
            log.error("Unexpected error occurred", e);
        }

        final var sendNotificationFuture = apnsClient.sendNotification(
                notificationHandler.buildNotification()
        );

        sendNotificationFuture.whenComplete((response, cause) -> {
            semaphore.release();
            if (Objects.nonNull(response)) {
                if (response.isAccepted()) {
                    notificationHandler.onSuccess();
                } else {
                    final String rejectionReason = response.getRejectionReason();
                    notificationHandler.onRejection(rejectionReason);
                }
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                log.warn("Push Notification sent by {} failed", this.getId(), cause);
                notificationHandler.onRejection(truncate(cause.getMessage(), 255));
            }
        }).exceptionally(e -> {
            log.error("Unexpected error occurred", e);
            return null;
        });
    }

    @Override
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

    public String getId() {
        return host + "[" + port + "]";
    }
}
