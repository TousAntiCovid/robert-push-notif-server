package fr.gouv.stopc.robert.pushnotif.scheduler.it;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.it.tools.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.repository.PushInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

import static fr.gouv.stopc.robert.pushnotif.scheduler.it.tools.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.it.tools.ItTools.getRandomNumberInRange;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@ActiveProfiles({ "dev" })
class VolumetryIntegrationTest {

    private static final int PUSH_NOTIF_COUNT = 100;

    private static final int WAITING_DURATION_SEC = 60;

    @Autowired
    PushInfoRepository pushInfoRepository;

    // This test class is useful to do test with volumetry
    // @Disabled
    @Test
    void should_correctly_sent_large_amount_of_notification_to_apns_servers() {

        // Given
        LongStream.rangeClosed(1, PUSH_NOTIF_COUNT).forEach(i -> {
            PushInfo push = PushInfo.builder()
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
                    .build();

            pushInfoRepository.saveAndFlush(push);
        });

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(
                PUSH_NOTIF_COUNT, Duration.ofSeconds(WAITING_DURATION_SEC)
        );

        assertThat(notificationSentToMainApnServer).hasSize(PUSH_NOTIF_COUNT);

        assertThat(pushInfoRepository.findAll()).hasSize(PUSH_NOTIF_COUNT)
                .allSatisfy(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .allSatisfy(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .allSatisfy(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(1));
    }

}