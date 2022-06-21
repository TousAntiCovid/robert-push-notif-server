package fr.gouv.stopc.robert.pushnotif.scheduler.data;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.NotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static com.eatthepath.pushy.apns.util.SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD;
import static com.eatthepath.pushy.apns.util.TokenUtil.sanitizeTokenString;
import static java.time.temporal.ChronoUnit.MINUTES;

@RequiredArgsConstructor
public class PushInfoNotificationHandler implements NotificationHandler<PushInfo> {

    private final PushInfo notificationData;

    private final PushInfoDao pushInfoDao;

    private final RobertPushServerProperties pushServerProperties;

    @Override
    public String getAppleToken() {
        return notificationData.getToken();
    }

    @Override
    public void onSuccess() {
        notificationData.setLastSuccessfulPush(Instant.now());
        notificationData.setSuccessfulPushSent(notificationData.getSuccessfulPushSent() + 1);
        pushInfoDao.updateSuccessFulPushedNotif(notificationData);
    }

    @Override
    public void onRejection(final String rejectionReason) {
        notificationData.setLastErrorCode(rejectionReason);
        notificationData.setLastFailurePush(Instant.now());
        notificationData.setFailedPushSent(notificationData.getFailedPushSent() + 1);
        pushInfoDao.updateFailurePushedNotif(notificationData);
    }

    @Override
    public void onTokenNotFound(final String rejectionReason) {
        notificationData.setActive(false);
        notificationData.setLastErrorCode(rejectionReason);
        notificationData.setLastFailurePush(Instant.now());
        notificationData.setFailedPushSent(notificationData.getFailedPushSent() + 1);
        pushInfoDao.updateFailurePushedNotif(notificationData);
    }

    @Override
    public SimpleApnsPushNotification buildNotification(final String topic) {

        final String payload = new SimpleApnsPayloadBuilder()
                .setContentAvailable(true)
                .setBadgeNumber(0)
                .build();

        return new SimpleApnsPushNotification(
                sanitizeTokenString(getAppleToken()).toLowerCase(),
                topic,
                payload,
                Instant.now().plus(DEFAULT_EXPIRATION_PERIOD),
                DeliveryPriority.IMMEDIATE,
                PushType.BACKGROUND
        );
    }

    public void updateNextPlannedPushToRandomTomorrow() {
        notificationData.setNextPlannedPush(generateDateTomorrowBetweenBounds(notificationData.getTimezone()));
        pushInfoDao.updateNextPlannedPushDate(notificationData);
    }

    private Instant generateDateTomorrowBetweenBounds(final String timezone) {

        final var random = ThreadLocalRandom.current();
        final var maxPushHour = pushServerProperties.getMaxPushHour();
        final var minPushHour = pushServerProperties.getMinPushHour();

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
