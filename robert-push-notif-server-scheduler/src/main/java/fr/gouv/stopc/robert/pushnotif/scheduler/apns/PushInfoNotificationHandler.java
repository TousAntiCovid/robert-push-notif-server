package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsNotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.model.PushInfo;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

import static org.apache.commons.lang3.StringUtils.truncate;

@RequiredArgsConstructor
public class PushInfoNotificationHandler implements ApnsNotificationHandler {

    private final PushInfo notificationData;

    private final PushInfoRepository pushInfoRepository;

    private final String apnsTopic;

    @Override
    public void onSuccess() {
        notificationData.setLastSuccessfulPush(Instant.now());
        notificationData.setSuccessfulPushSent(notificationData.getSuccessfulPushSent() + 1);
        pushInfoRepository.updateSuccessFulPushedNotif(notificationData);
    }

    @Override
    public void onRejection(final RejectionReason reason) {
        notificationData.setLastErrorCode(reason.getValue());
        notificationData.setLastFailurePush(Instant.now());
        notificationData.setFailedPushSent(notificationData.getFailedPushSent() + 1);
        pushInfoRepository.updateFailurePushedNotif(notificationData);
    }

    @Override
    public void onError(final Throwable cause) {
        notificationData.setLastErrorCode(truncate(cause.getMessage(), 255));
        notificationData.setLastFailurePush(Instant.now());
        notificationData.setFailedPushSent(notificationData.getFailedPushSent() + 1);
        pushInfoRepository.updateFailurePushedNotif(notificationData);
    }

    @Override
    public void disableToken() {
        notificationData.setActive(false);
    }
}
