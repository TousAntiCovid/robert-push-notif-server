package fr.gouv.stopc.robert.pushnotif.scheduler.it;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.stopc.robert.pushnotif.scheduler.it.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.it.APNsServersManager.awaitMainRejectedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.it.ItTools.getRandomNumberInRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@Slf4j
class NominalCaseIntegrationTest {

    public static final LocalDateTime TOMORROW_PLANNED_DATE = LocalDateTime
            .from(LocalDate.now().atStartOfDay().plusDays(1));

    @Autowired
    PushInfoToolsDao pushInfoToolsDao;

    @Test
    void should_correctly_manage_nominal_cases() {

        // Given
        loadData();

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(
                1, Duration.ofSeconds(30)
        );
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(
                1, Duration.ofSeconds(30)
        );

        assertThat(notificationSentToMainApnServer).hasSize(1);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);

        log.info("Check the status of the notification that has been correctly sent to APNs server");
        assertThat(pushInfoToolsDao.findByToken("A-TOK1111111111111111"))
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastSuccessfulPush())
                                .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1))
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
                                .isCloseTo(
                                        Instant.now().plus(Duration.ofDays(1)), within(
                                                30,
                                                ChronoUnit.SECONDS
                                        )
                                )
                )
                .satisfies(
                        notif -> assertThat(notif.getPayload())
                                .isEqualTo("{\"aps\":{\"badge\":0,\"content-available\":1}}")
                );

        log.info("This notification is not pushed because its planned date is in future");
        assertThat(pushInfoToolsDao.findByToken("FUTURE-1111111111111112"))
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush())
                                .isEqualTo(TOMORROW_PLANNED_DATE)
                );

        log.info("Check the status of the notification that has been rejected by APNs server");
        assertThat(pushInfoToolsDao.findByToken("987654321"))
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastFailurePush())
                                .isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isEqualTo("BadDeviceToken"))
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1))
                        )
                );
    }

    private void loadData() {
        PushInfo acceptedPushNotif = PushInfo.builder()
                .id(1L)
                .token("A-TOK1111111111111111")
                .locale("fr_FR")
                .timezone("Europe/Paris")
                .active(true)
                .deleted(false)
                .nextPlannedPush(
                        LocalDateTime.from(
                                LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                        .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                        )
                )
                .build();

        pushInfoToolsDao.insert(acceptedPushNotif);

        // This notification will be sent tomorrow not today
        PushInfo pushNotifInFuture = PushInfo.builder()
                .id(2L)
                .token("FUTURE-1111111111111112")
                .locale("fr_FR")
                .timezone("Europe/Paris")
                .active(true)
                .deleted(false)
                .nextPlannedPush(TOMORROW_PLANNED_DATE)
                .build();

        pushInfoToolsDao.insert(pushNotifInFuture);

        PushInfo badTokenNotif = PushInfo.builder()
                .id(3L)
                .token("987654321")
                .locale("fr_FR")
                .timezone("Europe/Paris")
                .active(true)
                .deleted(false)
                .nextPlannedPush(
                        LocalDateTime.from(
                                LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                        .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                        )
                )
                .build();

        pushInfoToolsDao.insert(badTokenNotif);

    }
}
