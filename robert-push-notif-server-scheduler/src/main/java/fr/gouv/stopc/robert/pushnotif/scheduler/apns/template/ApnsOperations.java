package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.NotificationHandler;

import java.time.Duration;

public interface ApnsOperations {

    <T> void sendNotification(NotificationHandler<T> handler);

    void waitUntilNoActivity(Duration toleranceDuration);
}
