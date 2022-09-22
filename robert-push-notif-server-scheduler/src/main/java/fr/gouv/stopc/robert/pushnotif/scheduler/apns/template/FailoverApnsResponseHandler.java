package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;

import java.util.List;

public interface FailoverApnsResponseHandler {

    /**
     * Called when the notification request is accepted.
     */
    void onSuccess();

    /**
     * Called when the notification request is rejected.
     *
     * @param reasons rejected push response messages
     */
    void onRejection(List<RejectionReason> reasons);

    /**
     * Called when the notification request fails before reaching Apple server.
     *
     * @param reason error message
     */
    void onError(Throwable reason);

    /**
     * Called when the notification request is rejected because one of inactive
     * rejection reasons.
     *
     * @param reasons rejected push response messages
     * @see RobertPushServerProperties.Apns#getInactiveRejectionReason()
     */
    void onInactive(List<RejectionReason> reasons);
}
