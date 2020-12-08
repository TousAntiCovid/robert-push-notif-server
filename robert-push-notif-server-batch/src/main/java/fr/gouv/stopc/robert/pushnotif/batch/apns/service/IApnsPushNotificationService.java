package fr.gouv.stopc.robert.pushnotif.batch.apns.service;

import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public interface IApnsPushNotificationService {

    PushInfo sendPushNotification(PushInfo push);

    public void close();
}
