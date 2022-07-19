package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RequiredArgsConstructor
public class FailoverApnsTemplate implements ApnsOperations {

    private final List<ApnsOperations> apnsDelegates;

    private final List<RejectionReason> inactiveRejectionReasons;

    @Override
    public void sendNotification(final NotificationHandler notificationHandler) {

        final var apnsClientsQueue = new ConcurrentLinkedQueue<>(apnsDelegates);

        sendNotification(notificationHandler, apnsClientsQueue);
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        apnsDelegates.parallelStream().forEach(it -> it.waitUntilNoActivity(toleranceDuration));
    }

    private void sendNotification(final NotificationHandler notificationHandler,
            final ConcurrentLinkedQueue<? extends ApnsOperations> queue) {

        final var client = queue.poll();

        if (client == null) {
            return;
        }
        final NotificationHandler multiApnsTemplateHandler = new NotificationHandler() {

            @Override
            public String getAppleToken() {
                return notificationHandler.getAppleToken();
            }

            @Override
            public void onSuccess() {
                notificationHandler.onSuccess();
            }

            @Override
            public void onRejection(final RejectionReason rejectionMessage) {

                if (inactiveRejectionReasons.contains(rejectionMessage)) {
                    // errors which means to try on another apn server
                    if (!queue.isEmpty()) {
                        // try next apn client in the queue
                        sendNotification(notificationHandler, queue);
                    } else {
                        // token was unsuccessful on every client, disable it
                        notificationHandler.disableToken();
                        notificationHandler.onRejection(rejectionMessage);
                    }
                } else {
                    notificationHandler.onRejection(rejectionMessage);
                }
            }

            @Override
            public void onError(final Throwable cause) {
                notificationHandler.onError(cause);
            }

            @Override
            public void disableToken() {
                notificationHandler.disableToken();
            }

            @Override
            public ApnsPushNotification buildNotification() {
                return notificationHandler.buildNotification();
            }
        };

        client.sendNotification(multiApnsTemplateHandler);
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
