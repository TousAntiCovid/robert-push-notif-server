package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.ItTools.getRandomNumberInRange;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenOnePushInfoSuchAs;
import static java.time.ZoneOffset.UTC;
import static java.util.stream.LongStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;

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
        rangeClosed(1, PUSH_NOTIF_COUNT).forEach(
                i -> givenOnePushInfoSuchAs(
                        PushInfo.builder()
                                .id(i)
                                .token(UUID.randomUUID().toString())
                                .locale("fr_FR")
                                .timezone("Europe/Paris")
                                .active(true)
                                .deleted(false)
                                .creationDate(Instant.now())
                                .nextPlannedPush(
                                        LocalDateTime.from(
                                                LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                                        .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                                        ).toInstant(UTC)
                                )
                                .build()
                )
        );

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(
                PUSH_NOTIF_COUNT, Duration.ofSeconds(WAITING_DURATION_SEC)
        );

        assertThat(notificationSentToMainApnServer).hasSize(PUSH_NOTIF_COUNT);

        assertThat(PsqlManager.findAll()).hasSize(PUSH_NOTIF_COUNT)
                .allSatisfy(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .allSatisfy(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .allSatisfy(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .allSatisfy(
                        pushInfo -> assertThat(pushInfo.getSuccessfulPushSent())
                                .as("successful push sent must be equal to 1").isEqualTo(1)
                )
                .allSatisfy(
                        pushInfo -> assertThat(pushInfo.getFailedPushSent()).as("failed push sent must be equal to 0")
                                .isEqualTo(0)
                );
    }
}
