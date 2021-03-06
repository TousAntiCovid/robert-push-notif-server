package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import java.time.Duration;

public interface ApnsOperations extends AutoCloseable {

    void sendNotification(NotificationHandler handler);

    void waitUntilNoActivity(Duration toleranceDuration);
}
