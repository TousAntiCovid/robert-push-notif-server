package fr.gouv.stopc.robert.pushnotif.batch.apns.service;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public interface IApnsPushNotificationService {

    PushInfo sendPushNotification(PushInfo push);

    public void close();
}
