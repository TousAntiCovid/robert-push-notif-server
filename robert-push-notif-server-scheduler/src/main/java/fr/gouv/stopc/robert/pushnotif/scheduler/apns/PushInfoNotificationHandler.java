package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsNotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.apache.commons.lang3.StringUtils.truncate;

@RequiredArgsConstructor
public class PushInfoNotificationHandler implements ApnsNotificationHandler {

    private final PushInfo notificationData;

    private final PushInfoDao pushInfoDao;

    private final String apnsTopic;

    private final int minPushHour;

    private final int maxPushHour;

    @Override
    public void onSuccess() {
        notificationData.setLastSuccessfulPush(Instant.now());
        notificationData.setSuccessfulPushSent(notificationData.getSuccessfulPushSent() + 1);
        pushInfoDao.updateSuccessFulPushedNotif(notificationData);
    }

    @Override
    public void onRejection(final RejectionReason reason) {
        notificationData.setLastErrorCode(reason.getValue());
        notificationData.setLastFailurePush(Instant.now());
        notificationData.setFailedPushSent(notificationData.getFailedPushSent() + 1);
        pushInfoDao.updateFailurePushedNotif(notificationData);
    }

    @Override
    public void onError(final Throwable cause) {
        notificationData.setLastErrorCode(truncate(cause.getMessage(), 255));
        notificationData.setLastFailurePush(Instant.now());
        notificationData.setFailedPushSent(notificationData.getFailedPushSent() + 1);
        pushInfoDao.updateFailurePushedNotif(notificationData);
    }

    @Override
    public void disableToken() {
        notificationData.setActive(false);
    }

    public void updateNextPlannedPushToRandomTomorrow() {
        notificationData.setNextPlannedPush(generateDateTomorrowBetweenBounds(notificationData.getTimezone()));
        pushInfoDao.updateNextPlannedPushDate(notificationData);
    }

    private Instant generateDateTomorrowBetweenBounds(final String timezone) {

        final var random = ThreadLocalRandom.current();

        final int durationBetweenHours;
        // In case config requires "between 6pm and 4am" which translates in minPushHour
        // = 18 and maxPushHour = 4
        if (maxPushHour < minPushHour) {
            durationBetweenHours = 24 - minPushHour + maxPushHour;
        } else {
            durationBetweenHours = maxPushHour - minPushHour;
        }

        return ZonedDateTime.now(ZoneId.of(timezone)).plusDays(1)
                .withHour(random.nextInt(durationBetweenHours) + minPushHour % 24)
                .withMinute(random.nextInt(60))
                .toInstant()
                .truncatedTo(MINUTES);
    }
}
