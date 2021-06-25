package fr.gouv.stopc.robert.pushnotif.batch.apns.service;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

import java.util.concurrent.ExecutionException;

public interface IApnsPushNotificationService {

    PushInfo sendPushNotification(PushInfo push);

    public void close() throws ExecutionException, InterruptedException;
}
