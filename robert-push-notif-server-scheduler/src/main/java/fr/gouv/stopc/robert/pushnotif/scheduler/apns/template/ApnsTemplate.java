package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRejectionCode.NONE;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRejectionCode.getRejectionCodeOrUnknown;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.*;
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

    private final MetricsService metricsService;

    @ToString.Include
    private final String host;

    @ToString.Include
    private final int port;

    public ApnsTemplate(final ApnsClient apnsClient,
            final MetricsService metricsService,
            final String host,
            final int port,
            final MeterRegistry meterRegistry) {
        this.apnsClient = apnsClient;
        this.metricsService = metricsService;
        this.host = host;
        this.port = port;
    }

    public void sendNotification(final NotificationHandler notificationHandler) {
        final var pushRequestChrono = Timer.start();

        pendingNotifications.incrementAndGet();

        final var sendNotificationFuture = apnsClient.sendNotification(notificationHandler.buildNotification());

        sendNotificationFuture
                .whenComplete((response, cause) -> {

                    pendingNotifications.decrementAndGet();

                    if (Objects.nonNull(response)) {
                        if (response.isAccepted()) {
                            pushRequestChrono.stop(metricsService.getTimer(host, String.valueOf(port), ACCEPTED, NONE));
                            notificationHandler.onSuccess();
                        } else {
                            String rejectionReason = response.getRejectionReason()
                                    .orElse("Notification request rejected for unknown reason");
                            pushRequestChrono.stop(
                                    metricsService.getTimer(
                                            host, String.valueOf(port), REJECTED,
                                            getRejectionCodeOrUnknown(rejectionReason)
                                    )
                            );
                            notificationHandler.onRejection(rejectionReason);
                        }
                    } else {
                        // Something went wrong when trying to send the notification to the
                        // APNs server. Note that this is distinct from a rejection from
                        // the server, and indicates that something went wrong when actually
                        // sending the notification or waiting for a reply.
                        log.warn("Push Notification sent by {}:{} failed", host, port, cause);
                        pushRequestChrono.stop(metricsService.getTimer(host, String.valueOf(port), ERROR, NONE));
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
}
