package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@RequiredArgsConstructor
public class FailoverApnsTemplate implements ApnsOperations {

    private final List<ApnsOperations> apnsDelegates;

    private final List<String> inactiveRejectionReasons;

    @Override
    public void sendNotification(NotificationHandler notificationHandler) {

        final var apnsClientsQueue = new ConcurrentLinkedQueue<>(apnsDelegates);

        sendNotification(notificationHandler, apnsClientsQueue);
    }

    @Override
    public void waitUntilNoActivity(Duration toleranceDuration) {
        apnsDelegates.parallelStream().forEach(it -> it.waitUntilNoActivity(toleranceDuration));
    }

    private <T> void sendNotification(final NotificationHandler notificationHandler,
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
            public void onRejection(String rejectionMessage) {

                if (inactiveRejectionReasons.contains(rejectionMessage)) {
                    // errors which means to try on another apn server
                    if (queue.size() > 0) {
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
            public void onError(String reason) {
                notificationHandler.onError(reason);
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
        apnsDelegates.parallelStream().forEach(apnsTemplate -> {
            try {
                apnsTemplate.close();
            } catch (Exception e) {
                log.error("Unable to close {} gracefully", apnsTemplate, e);
            }
        });
    }
}
