package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;

public interface NotificationHandler {

    /**
     * @return Apple push notification device token
     */
    String getAppleToken();

    /**
     * Called when the notification request is accepted
     */
    void onSuccess();

    /**
     * Called when the notification request is rejected
     * 
     * @param rejectionMessage rejected push notification request response message
     */
    void onRejection(final String rejectionMessage);

    /**
     * Called when the notification request fails before reaching Apple server.
     *
     * @param reason error message
     */
    void onError(final String reason);

    /**
     * Called when the notification request is rejected on every configured APN
     * server. In this app context, we have configured multiple APN servers. When a
     * push notif request fails because of specific configured errors:
     * 
     * @see RobertPushServerProperties.Apns#inactiveRejectionReason The app tries
     *      again on the next configured APN server. If the request fails on every
     *      APN server, this method is called.
     * @param rejectionMessage rejected push notification request response message
     */
    void disableToken();

    /**
     * @param topic: Apple Push Notification topic
     * @return Push Notification for Apple Push Notification service
     * @see ApnsClient#sendNotification(ApnsPushNotification)
     */
    ApnsPushNotification buildNotification();
}
