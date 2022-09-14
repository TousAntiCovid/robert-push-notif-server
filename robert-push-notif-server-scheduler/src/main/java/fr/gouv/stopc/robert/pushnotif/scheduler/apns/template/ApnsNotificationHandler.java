package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;

public interface ApnsNotificationHandler {

    /**
     * Called when the notification request is accepted
     */
    void onSuccess();

    /**
     * Called when the notification request is rejected
     * 
     * @param reason rejected push notification request response message
     */
    void onRejection(final RejectionReason reason);

    /**
     * Called when the notification request fails before reaching Apple server.
     *
     * @param reason error message
     */
    void onError(final Throwable reason);

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
}
