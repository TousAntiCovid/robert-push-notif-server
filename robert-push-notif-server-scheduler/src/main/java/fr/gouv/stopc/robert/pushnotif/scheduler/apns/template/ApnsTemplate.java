package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.truncate;

/**
 * Used to identify the APN server host & port when an error occurs
 */
@Slf4j
@ToString
@RequiredArgsConstructor
public class ApnsTemplate implements ApnsOperations {

    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    private final ApnsClient apnsClient;

    @ToString.Include
    private final String host;

    @ToString.Include
    private final int port;

    private final Timer notifDurationTimer;

    public ApnsTemplate(final ApnsClient apnsClient,
            final String host,
            final int port,
            final MeterRegistry meterRegistry) {
        this.apnsClient = apnsClient;
        this.host = host;
        this.port = port;
        this.notifDurationTimer = Timer.builder("pushy.notifications.sent.timer")
                .tags("host", host, "port", "" + port)
                .register(meterRegistry);
    }

    public void sendNotification(final NotificationHandler notificationHandler) {
        final var pushRequestChrono = Timer.start();

        pendingNotifications.incrementAndGet();

        final var sendNotificationFuture = apnsClient.sendNotification(notificationHandler.buildNotification());

        sendNotificationFuture
                .whenComplete((response, cause) -> {
                    pushRequestChrono.stop(notifDurationTimer);

                    pendingNotifications.decrementAndGet();

                    if (Objects.nonNull(response)) {
                        if (response.isAccepted()) {
                            notificationHandler.onSuccess();
                        } else {
                            notificationHandler.onRejection(
                                    response.getRejectionReason()
                                            .orElse("Notification request rejected for unknown reason")
                            );
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
            log.info("it remains {} pending notifications with client {}", pendingNotifications.get(), this);
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
        apnsClient.close()
                .get(1, MINUTES);
    }

    @Override
    public String toString() {
        return String.format("%s:%d", host, port);
    }
}
