package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsNotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsOperations;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.PushInfoRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

import static com.eatthepath.pushy.apns.util.SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD;
import static com.eatthepath.pushy.apns.util.TokenUtil.sanitizeTokenString;

@Slf4j
@Service
@RequiredArgsConstructor
public class Scheduler {

    private final PushInfoRepository pushInfoRepository;

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsOperations apnsTemplate;

    @Scheduled(fixedDelayString = "${robert.push.server.scheduler.delay-in-ms}")
    @Timed(value = "push.notifier.duration", description = "on going export duration", longTask = true)
    @Counted(value = "push.notifier.calls", description = "count each time the scheduler sending notifications is triggered")
    public void sendNotifications() {

        pushInfoRepository.forEachNotificationToBeSent(pushInfo -> {
            // set the next planned push to be sure the notification could not be sent 2
            // times the same day
            pushInfoRepository.updateNextPlannedPushDate(
                    pushInfo.withPushDateTomorrowBetween(
                            robertPushServerProperties.getMinPushHour(),
                            robertPushServerProperties.getMaxPushHour()
                    )
            );
            final var notification = buildNotification(pushInfo.getToken());
            apnsTemplate.sendNotification(notification, new ApnsNotificationHandler() {

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
            });
        });

        apnsTemplate.waitUntilNoActivity(Duration.ofSeconds(10));
    }

    public SimpleApnsPushNotification buildNotification(final String apnsToken) {
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
}
