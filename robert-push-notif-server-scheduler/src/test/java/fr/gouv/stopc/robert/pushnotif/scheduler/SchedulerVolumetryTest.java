package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.ItTools.getRandomNumberInRange;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoWith;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.LongStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
@ActiveProfiles({ "dev" })
class SchedulerVolumetryTest {

    private static final int PUSH_NOTIF_COUNT = 100;

    private static final int WAITING_DURATION_SEC = 60;

    // This test class is useful to do test with volumetry
    // @Disabled
    @Test
    void should_correctly_send_large_amount_of_notification_to_apns_servers() {

        // Given
        rangeClosed(1, PUSH_NOTIF_COUNT).forEach(i -> {
            givenPushInfoWith(
                    b -> b.id(i)
                            .token(UUID.randomUUID().toString())
                            .nextPlannedPush(
                                    LocalDateTime.from(
                                            LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                                    .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                                    ).toInstant(UTC)
                            )
            );
        });

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(
                PUSH_NOTIF_COUNT, Duration.ofSeconds(WAITING_DURATION_SEC)
        );

        assertThat(notificationSentToMainApnServer).hasSize(PUSH_NOTIF_COUNT);

        assertThat(PsqlManager.findAll()).hasSize(PUSH_NOTIF_COUNT)
                .extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastFailurePush,
                        PushInfo::getLastErrorCode
                )
                .containsOnly(tuple(true, false, 1, 0, null, null));
    }
}
