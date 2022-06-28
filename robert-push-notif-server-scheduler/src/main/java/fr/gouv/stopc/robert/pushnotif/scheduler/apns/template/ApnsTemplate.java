package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.NotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.truncate;

/**
 * Used to identify the APN server host & port when an error occurs
 */
@Slf4j
@RequiredArgsConstructor
public class ApnsTemplate implements ApnsOperations {

    private final ApnsClient apnsClient;

    private final String host;

    private final int port;

    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    public <T> void sendNotification(final NotificationHandler notificationHandler) {
        pendingNotifications.incrementAndGet();

        final var sendNotificationFuture = apnsClient.sendNotification(
                notificationHandler.buildNotification()
        );

        sendNotificationFuture.whenComplete((response, cause) -> {
            pendingNotifications.decrementAndGet();
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
                log.warn("Push Notification sent by {}:{} failed", host, port, cause);
                notificationHandler.onRejection(truncate(cause.getMessage(), 255));
            }
        }).exceptionally(e -> {
            log.error("Unexpected error occurred", e);
            return null;
        });
    }

    @Override
    public void waitUntilNoActivity(Duration toleranceDuration) {
        do {
            log.info("it remains {} pending notifications with client {}:{}", pendingNotifications.get(), host, port);
            try {
                SECONDS.sleep(toleranceDuration.getSeconds());
            } catch (InterruptedException e) {
                log.warn("Unable to wait until all notifications are sent", e);
            }
        } while (pendingNotifications.get() != 0);
    }
}
