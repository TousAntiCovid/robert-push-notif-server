package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import lombok.RequiredArgsConstructor;

/**
 * Used to identify the APN server host & port when an error occurs
 */
@RequiredArgsConstructor
public class ApnsClientDecorator {

    private final ApnsClient apnsClient;

    private final String host;

    private final int port;

    public <T extends ApnsPushNotification> PushNotificationFuture<T, PushNotificationResponse<T>> sendNotification(
            final T notification) {
        return apnsClient.sendNotification(notification);
    }

    public String getId() {
        return host + "[" + port + "]";
    }
}
