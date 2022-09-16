package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An APNS template able to defer notification sending to fallback servers
 * depending on the reason returned from the previous server.
 */
@Slf4j
@RequiredArgsConstructor
public class FailoverApnsTemplate implements ApnsOperations {

    private final List<ApnsOperations> apnsDelegates;

    private final List<RejectionReason> inactiveRejectionReasons;

    @Override
    public void sendNotification(final ApnsPushNotification notification,
            final ApnsNotificationHandler notificationHandler) {

        final var apnsClientsQueue = new ConcurrentLinkedQueue<>(apnsDelegates);

        sendNotification(notification, notificationHandler, apnsClientsQueue);
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        apnsDelegates.parallelStream().forEach(it -> it.waitUntilNoActivity(toleranceDuration));
    }

    private void sendNotification(final ApnsPushNotification notification,
            final ApnsNotificationHandler notificationHandler,
            final ConcurrentLinkedQueue<? extends ApnsOperations> queue) {

        final var client = queue.poll();
        if (client != null) {
            client.sendNotification(notification, new DelegateNotificationHandler(notificationHandler) {

                @Override
                public void onRejection(final RejectionReason reason) {
                    if (inactiveRejectionReasons.contains(reason)) {
                        // rejection reason means we must try on next APN server
                        if (!queue.isEmpty()) {
                            // try next apn client in the queue
                            sendNotification(notification, notificationHandler, queue);
                        } else {
                            // notification was rejected on every client, then disable token
                            super.disableToken();
                            super.onRejection(reason);
                        }
                    } else {
                        // rejection reason means the notification must not be attempted on next APN
                        // server
                        super.onRejection(reason);
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        apnsDelegates.parallelStream().forEach(delegate -> {
            try {
                delegate.close();
            } catch (Exception e) {
                log.error("Unable to close {} gracefully", delegate, e);
            }
        });
    }
}
