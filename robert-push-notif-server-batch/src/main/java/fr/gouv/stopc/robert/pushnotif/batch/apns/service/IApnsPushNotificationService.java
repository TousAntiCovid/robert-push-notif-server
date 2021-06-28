package fr.gouv.stopc.robert.pushnotif.batch.apns.service;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public interface IApnsPushNotificationService {

    PushInfo sendPushNotification(PushInfo push);

    public void close() throws ExecutionException, InterruptedException;

    public void initApnsClient() throws IOException, NoSuchAlgorithmException, InvalidKeyException;
}
