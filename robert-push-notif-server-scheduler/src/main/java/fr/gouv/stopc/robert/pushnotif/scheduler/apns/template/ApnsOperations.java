package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import java.time.Duration;
import java.util.List;

public interface ApnsOperations extends AutoCloseable {

    void sendNotification(NotificationHandler handler, List<String> rejections);

    void waitUntilNoActivity(Duration toleranceDuration);

    String getName();
}
