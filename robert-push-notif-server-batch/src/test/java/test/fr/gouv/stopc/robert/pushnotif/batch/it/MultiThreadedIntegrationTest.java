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

    @Test
    void should_correctly_sent_notification_to_apns_servers() throws Exception {

        // Given
        loadData();

        // When
        JobExecution jobExecution = this.jobLauncherTestUtils.launchJob();

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(1000);

        assertThat(notificationSentToMainApnServer).hasSize(1000);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions())
                .filteredOn(s -> s.getStatus().equals(BatchStatus.COMPLETED))
                .hasSize(11);
        assertThat(jobExecution.getStepExecutions())
                .filteredOn(s -> s.getStepName().equalsIgnoreCase("Step1"))
                .extracting("writeCount").containsExactly(1000);

        assertThat(pushInfoRepository.findAll()).hasSize(1000)
                .anySatisfy(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .anySatisfy(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .anySatisfy(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .anySatisfy(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .anySatisfy(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .anySatisfy(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(1))
                .anySatisfy(
                        pushInfo -> assertThat(pushInfo.getLastSuccessfulPush())
                                .isCloseTo(Date.from(Instant.now()), 10_000)
                )
                .anySatisfy(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                Date.from(LocalDate.now().atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC))
                        )
                );
    }

    private void loadData() {
        LongStream.rangeClosed(1, 1000L).forEach(i -> {
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
