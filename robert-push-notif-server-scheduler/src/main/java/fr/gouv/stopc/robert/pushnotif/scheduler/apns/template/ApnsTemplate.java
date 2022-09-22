package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.UNKNOWN;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Used to identify the APN server host & port when an error occurs
 */
@Slf4j
@RequiredArgsConstructor
public class ApnsTemplate implements ApnsOperations<ApnsResponseHandler> {

    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    private final ApnsServerCoordinates serverCoordinates;

    private final ApnsClient apnsClient;

    private final List<RejectionReason> inactiveRejectionReasons;

    public void sendNotification(final ApnsPushNotification notification,
            final ApnsResponseHandler responseHandler) {

        pendingNotifications.incrementAndGet();
        final var sendNotificationFuture = apnsClient.sendNotification(notification);

        sendNotificationFuture.whenComplete((response, cause) -> {
            pendingNotifications.decrementAndGet();
            if (response != null) {
                if (response.isAccepted()) {
                    responseHandler.onSuccess();
                } else {
                    final var rejection = response.getRejectionReason()
                            .map(RejectionReason::fromValue)
                            .orElse(UNKNOWN);
                    if (inactiveRejectionReasons.contains(rejection)) {
                        responseHandler.onInactive(rejection);
                    } else {
                        responseHandler.onRejection(rejection);
                    }
                }
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                responseHandler.onError(cause);
            }
        }).exceptionally(e -> {
            log.error("Unexpected error occurred", e);
            return null;
        });
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        do {
            try {
                SECONDS.sleep(toleranceDuration.getSeconds());
            } catch (InterruptedException e) {
                log.warn("Unable to wait until all notifications are sent", e);
            }
        } while (pendingNotifications.get() != 0);
        log.info("{} has no more pending notifications");
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down {}, gracefully waiting 1 minute", this);
        apnsClient.close().get(1, MINUTES);
        log.info("{} is stopped", this);
    }

    @Override
    public String toString() {
        return String.format("ApnsTemplate(%s)", serverCoordinates);
    }
}
