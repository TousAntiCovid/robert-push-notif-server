package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRejectionReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRejectionReason.UNKNOWN;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Used to identify the APN server host & port when an error occurs
 */
@Slf4j
@RequiredArgsConstructor
public class ApnsTemplate implements ApnsOperations {

    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    private final ApnsClient apnsClient;

    public void sendNotification(final NotificationHandler notificationHandler) {

        final var sendNotificationFuture = apnsClient.sendNotification(notificationHandler.buildNotification());

        sendNotificationFuture.whenComplete((response, cause) -> {
            if (response != null) {
                if (response.isAccepted()) {
                    notificationHandler.onSuccess();
                } else {
                    notificationHandler.onRejection(
                            response.getRejectionReason()
                                    .map(ApnsRejectionReason::fromValue)
                                    .orElse(UNKNOWN)
                    );
                }
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                notificationHandler.onError(cause);
            }
        }).exceptionally(e -> {
            log.error("Unexpected error occurred", e);
            return null;
        });
    }

    @Override
    public void waitUntilNoActivity(Duration toleranceDuration) {
        do {
            try {
                SECONDS.sleep(toleranceDuration.getSeconds());
            } catch (InterruptedException e) {
                log.warn("Unable to wait until all notifications are sent", e);
            }
        } while (pendingNotifications.get() != 0);
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down {}, gracefully waiting 1 minute", this);
        apnsClient.close().get(1, MINUTES);
    }
}
