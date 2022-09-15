package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsNotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.model.PushInfo;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PushInfoNotificationHandler implements ApnsNotificationHandler {

    private final PushInfo pushInfo;

    private final PushInfoRepository pushInfoRepository;

    @Override
    public void onSuccess() {
        pushInfoRepository.updateSuccessfulPushSent(pushInfo.getId());
    }

    @Override
    public void onRejection(final RejectionReason reason) {
        pushInfoRepository.updateFailure(pushInfo.getId(), reason.getValue());
    }

    @Override
    public void onError(final Throwable cause) {
        pushInfoRepository.updateFailure(pushInfo.getId(), cause.getMessage());
    }

    @Override
    public void disableToken() {
        pushInfoRepository.disable(pushInfo.getId());
    }
}
