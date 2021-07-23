package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

public class TacApnsClient {

    private final ApnsClient apnsClient;

    private final String host;

    private final int port;

    public TacApnsClient(ApnsClient apnsClient, String host, int port) {
        this.apnsClient = apnsClient;
        this.host = host;
        this.port = port;
    }

    public <T extends ApnsPushNotification> PushNotificationFuture<T, PushNotificationResponse<T>> sendNotification(
            final T notification) {
        return apnsClient.sendNotification(notification);
    }

    public String getId() {
        return host + "[" + port + "]";
    }

}
