package test.fr.gouv.stopc.robert.pushnotif.batch.it;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static test.fr.gouv.stopc.robert.pushnotif.batch.it.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static test.fr.gouv.stopc.robert.pushnotif.batch.it.ItTools.getRandomNumberInRange;

@IntegrationTest
@SpringBatchTest
class MultiThreadedIntegrationTest {

    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    IPushInfoService pushInfoService;

    @Autowired
    PushInfoRepository pushInfoRepository;

    private static final int PUSH_NOTIF_COUNT = 1007;

    @Test
    void should_correctly_sent_notification_to_apns_servers() throws Exception {

        // Given
        loadData();

        // When
        JobExecution jobExecution = this.jobLauncherTestUtils.launchJob();

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(
                PUSH_NOTIF_COUNT
        );

        assertThat(notificationSentToMainApnServer).hasSize(PUSH_NOTIF_COUNT);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions())
                .filteredOn(s -> s.getStatus().equals(BatchStatus.COMPLETED))
                .hasSize(11);
        assertThat(jobExecution.getStepExecutions())
                .filteredOn(s -> s.getStepName().equalsIgnoreCase("Step1"))
                .extracting("writeCount").containsExactly(PUSH_NOTIF_COUNT);

        assertThat(pushInfoRepository.findAll()).hasSize(PUSH_NOTIF_COUNT)
                .allSatisfy(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .allSatisfy(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .allSatisfy(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .allSatisfy(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(1))
                .allSatisfy(
                        pushInfo -> assertThat(pushInfo.getLastSuccessfulPush())
                                .isCloseTo(Date.from(Instant.now()), 60_000)
                )
                .allSatisfy(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                Date.from(LocalDate.now().atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC))
                        )
                );
    }

    private void loadData() {
        LongStream.rangeClosed(1, PUSH_NOTIF_COUNT).forEach(i -> {
            PushInfo push = PushInfo.builder()
                    .token(UUID.randomUUID().toString())
                    .locale("fr_FR")
                    .timezone("Europe/Paris")
                    .active(true)
                    .deleted(false)
                    .nextPlannedPush(
                            Date.from(
                                    LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                            .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                                            .toInstant(ZoneOffset.UTC)
                            )
                    )
                    .build();

            pushInfoService.createOrUpdate(push);
        });
    }
}
