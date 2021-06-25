package test.fr.gouv.stopc.robert.pushnotif.batch.it;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static test.fr.gouv.stopc.robert.pushnotif.batch.it.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static test.fr.gouv.stopc.robert.pushnotif.batch.it.APNsServersManager.awaitMainRejectedQueueContainsAtLeast;
import static test.fr.gouv.stopc.robert.pushnotif.batch.it.ItTools.getRandomNumberInRange;

@IntegrationTest
@SpringBatchTest
@Slf4j
class NominalCaseIntegrationTest {

    public static final Date TOMORROW_PLANNED_DATE = Date.from(
            LocalDate.now().atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC)
    );

    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    IPushInfoService pushInfoService;

    @Autowired
    PushInfoRepository pushInfoRepository;

    @Test
    void should_correctly_manage_nominal_cases() throws Exception {

        // Given
        loadData();

        // When
        JobExecution jobExecution = this.jobLauncherTestUtils.launchJob();

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);

        assertThat(notificationSentToMainApnServer).hasSize(1);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        log.info("Check the status of the notification that has been correctly sent to APNs server");
        assertThat(pushInfoRepository.findByToken("A-TOK1111111111111111").get())
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastSuccessfulPush())
                                .isCloseTo(Date.from(Instant.now()), 10_000)
                )
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                Date.from(LocalDate.now().atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC))
                        )
                );

        log.info("Check the content of the notification received on the APNs server side");
        assertThat(notificationSentToMainApnServer.get(0))
                .satisfies(notif -> assertThat(notif.getPushType()).isEqualTo(PushType.BACKGROUND))
                .satisfies(notif -> assertThat(notif.getPriority()).isEqualTo(DeliveryPriority.IMMEDIATE))
                .satisfies(notif -> assertThat(notif.getToken()).isEqualTo("a1111111111111111"))
                .satisfies(notif -> assertThat(notif.getTopic()).isEqualTo("test"))
                .satisfies(
                        notif -> assertThat(notif.getExpiration())
                                .isCloseTo(Instant.now().plus(Duration.ofDays(1)), within(30, ChronoUnit.SECONDS))
                )
                .satisfies(
                        notif -> assertThat(notif.getPayload())
                                .isEqualTo("{\"aps\":{\"badge\":0,\"content-available\":1}}")
                );

        log.info("This notification is not pushed because its planned date is in future");
        assertThat(pushInfoRepository.findByToken("FUTURE-1111111111111112").get())
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush().getTime())
                                .isEqualTo(TOMORROW_PLANNED_DATE.getTime())
                );

        log.info("Check the status of the notification that has been rejected by APNs server");
        assertThat(pushInfoRepository.findByToken("987654321").get())
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastFailurePush())
                                .isCloseTo(Date.from(Instant.now()), 10_000)
                )
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isEqualTo("BadDeviceToken"))
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                Date.from(LocalDate.now().atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC))
                        )
                );
    }

    private void loadData() {
        PushInfo acceptedPushNotif = PushInfo.builder()
                .token("A-TOK1111111111111111")
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

        pushInfoService.createOrUpdate(acceptedPushNotif);

        // This notification will be sent tomorrow not today
        PushInfo pushNotifInFuture = PushInfo.builder()
                .token("FUTURE-1111111111111112")
                .locale("fr_FR")
                .timezone("Europe/Paris")
                .active(true)
                .deleted(false)
                .nextPlannedPush(TOMORROW_PLANNED_DATE)
                .build();

        pushInfoService.createOrUpdate(pushNotifInFuture);

        PushInfo badTokenNotif = PushInfo.builder()
                .token("987654321")
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

        pushInfoService.createOrUpdate(badTokenNotif);

    }
}
