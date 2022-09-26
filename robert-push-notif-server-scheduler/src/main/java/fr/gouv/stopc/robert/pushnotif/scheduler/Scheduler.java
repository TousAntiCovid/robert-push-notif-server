package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsOperations;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.FailoverApnsResponseHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.model.PushInfo;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.eatthepath.pushy.apns.util.SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD;
import static com.eatthepath.pushy.apns.util.TokenUtil.sanitizeTokenString;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.joining;

@Slf4j
@Service
@RequiredArgsConstructor
public class Scheduler {

    private final PushInfoRepository pushInfoRepository;

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsOperations<FailoverApnsResponseHandler> apnsTemplate;

    @Scheduled(fixedDelayString = "${robert.push.server.scheduler.delay-in-ms}")
    @Timed(value = "push.notifier.duration", description = "on going export duration", longTask = true)
    @Counted(value = "push.notifier.calls", description = "count each time the scheduler sending notifications is triggered")
    public void sendNotifications() {
        log.info("start notification task");
        final var count = new AtomicInteger(0);
        pushInfoRepository.forEachNotificationToBeSent(pushInfo -> {
            // set the next planned push to be sure the notification could not be sent 2
            // times the same day
            log.info("update next planned push for {}", pushInfo.getToken());
            updateNextPlannedPush(pushInfo);
            final var notification = buildWakeUpNotification(pushInfo.getToken());
            log.info("submit notification for {}", pushInfo.getToken());
            apnsTemplate.sendNotification(notification, new WakeUpDeviceResponseHandler(pushInfo));
        });

        apnsTemplate.waitUntilNoActivity(robertPushServerProperties.getBatchTerminationGraceTime());
        log.info("end notification task");
    }

    /**
     * Updates the registered token with a new notification instant set to tomorrow.
     */
    private void updateNextPlannedPush(final PushInfo pushInfo) {
        final var nextPushDate = generatePushDateTomorrowBetween(
                robertPushServerProperties.getMinPushHour(),
                robertPushServerProperties.getMaxPushHour(),
                ZoneId.of(pushInfo.getTimezone())
        );
        pushInfoRepository.updateNextPlannedPushDate(pushInfo.getId(), nextPushDate);
    }

    /**
     * Builds the {@link com.eatthepath.pushy.apns.ApnsPushNotification} to be sent
     * to the Apple server.
     */
    private SimpleApnsPushNotification buildWakeUpNotification(final String apnsToken) {
        final var payload = new SimpleApnsPayloadBuilder()
                .setContentAvailable(true)
                .setBadgeNumber(0)
                .build();

        return new SimpleApnsPushNotification(
                sanitizeTokenString(apnsToken).toLowerCase(),
                robertPushServerProperties.getApns().getTopic(),
                payload,
                Instant.now().plus(DEFAULT_EXPIRATION_PERIOD),
                DeliveryPriority.IMMEDIATE,
                PushType.BACKGROUND
        );
    }

    /**
     * Generates a random instant tomorrow between the given hour bounds for the
     * specified timezone.
     * <p>
     * minPushHour can be greater than maxPushHour: its means notification period
     * starts this evening and ends tommorrow. For instance min=20 and max=7 means
     * notifications are send between today at 20:00 and tomorrow at 6:59.
     */
    static Instant generatePushDateTomorrowBetween(final int minPushHour, final int maxPushHour,
            final ZoneId timezone) {
        final var random = ThreadLocalRandom.current();
        final int durationBetweenHours;
        // In case config requires "between 6pm and 4am" which translates in minPushHour
        // = 18 and maxPushHour = 4
        if (maxPushHour < minPushHour) {
            durationBetweenHours = 24 - minPushHour + maxPushHour;
        } else {
            durationBetweenHours = maxPushHour - minPushHour;
        }
        return ZonedDateTime.now(timezone).plusDays(1)
                .withHour((random.nextInt(durationBetweenHours) + minPushHour) % 24)
                .withMinute(random.nextInt(60))
                .toInstant()
                .truncatedTo(MINUTES);
    }

    /**
     * Handles notification request response.
     */
    @RequiredArgsConstructor
    private class WakeUpDeviceResponseHandler implements FailoverApnsResponseHandler {

        private final PushInfo pushInfo;

        @Override
        public void onSuccess() {
            pushInfoRepository.updateSuccessfulPushSent(pushInfo.getId());
            log.info("success for {}", pushInfo.getToken());
        }

        @Override
        public void onRejection(final List<RejectionReason> reasons) {
            pushInfoRepository.updateFailure(pushInfo.getId(), concat(reasons));
            log.info("rejection for {}", pushInfo.getToken());
        }

        @Override
        public void onError(final Throwable cause) {
            pushInfoRepository.updateFailure(pushInfo.getId(), cause.getMessage());
            log.info("error for {}", pushInfo.getToken());
        }

        @Override
        public void onInactive(final List<RejectionReason> reasons) {
            pushInfoRepository.updateFailure(pushInfo.getId(), concat(reasons));
            pushInfoRepository.disable(pushInfo.getId());
            log.info("inactive for {}", pushInfo.getToken());
        }

        private String concat(List<RejectionReason> reasons) {
            return reasons.stream()
                    .map(RejectionReason::getValue)
                    .collect(joining(","));
        }
    }
}
