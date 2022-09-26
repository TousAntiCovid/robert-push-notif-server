package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;

import java.time.Duration;

public interface ApnsOperations<T> extends AutoCloseable {

    void sendNotification(ApnsPushNotification notification, T handler);

    void waitUntilNoActivity(Duration toleranceDuration);
}
