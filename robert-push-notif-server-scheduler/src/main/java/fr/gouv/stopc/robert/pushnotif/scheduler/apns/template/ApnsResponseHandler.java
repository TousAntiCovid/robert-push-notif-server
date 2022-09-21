package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;

public interface ApnsResponseHandler {

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
     * Called when the notification request is rejected because one of inactive
     * rejection reasons.
     *
     * @param reason rejected push notification request response message
     * @see RobertPushServerProperties.Apns#getInactiveRejectionReason()
     */
    void onInactive(RejectionReason reason);
}
