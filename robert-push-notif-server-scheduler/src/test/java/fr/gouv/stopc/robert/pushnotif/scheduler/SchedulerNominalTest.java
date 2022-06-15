package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainRejectedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.ItTools.getRandomNumberInRange;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenOnePushInfoSuchAs;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@ActiveProfiles({ "dev", "one-apns-server" })
@DirtiesContext
class SchedulerNominalTest {

    public static final Instant TOMORROW_PLANNED_DATE = LocalDateTime
            .from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC);

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response() {

        // Given
        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(1L)
                        .token("A-TOK1111111111111111")
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
        );

        // This notification will be sent tomorrow not today
        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(2L)
                        .token("FUTURE-1111111111111112")
                        .locale("fr_FR")
                        .timezone("Europe/Paris")
                        .active(true)
                        .deleted(false)
                        .nextPlannedPush(TOMORROW_PLANNED_DATE)
                        .build()
        );
        // When - triggering of the scheduled task

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(0);

        assertThat(notificationSentToMainApnServer).hasSize(1);
        assertThat(notificationRejectedByMainApnServer).hasSize(0);

        assertThat(PsqlManager.findByToken("A-TOK1111111111111111"))
                .as("Check the status of the notification that has been correctly sent to APNs server")
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastFailurePush()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isNull())
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastSuccessfulPush())
                                .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                        )
                );

        assertThat(notificationSentToMainApnServer.get(0))
                .as("Check the content of the notification received on the APNs server side")
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

        assertThat(PsqlManager.findByToken("FUTURE-1111111111111112"))
                .as("This notification is not pushed because its planned date is in future")
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
    }

    @Test
    void should_deactivate_notification_when_apns_server_replies_with_is_invalid_token_reason() {
        // Given
        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(3L)
                        .token("987654321")
                        .locale("fr_FR")
                        .timezone("Europe/Paris")
                        .active(true)
                        .deleted(false)
                        .creationDate(Instant.now())
                        .nextPlannedPush(
                                LocalDateTime.from(
                                        LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                                .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                                )
                                        .toInstant(UTC)
                        )
                        .build()
        );
        // When - triggering of the scheduled task

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);

        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);

        assertThat(PsqlManager.findByToken("987654321"))
                .as("Check the status of the notification that has been rejected by APNs server - notif is deactivated")
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastFailurePush())
                                .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isEqualTo("BadDeviceToken"))
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1))
                                        .toInstant(UTC)
                        )
                );
    }

    @Test
    void should_not_deactivate_notification_when_apns_server_replies_with_no_invalid_token_reason() {
        // Given
        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(4L)
                        .token("112233445566")
                        .locale("fr_FR")
                        .timezone("Europe/Paris")
                        .active(true)
                        .deleted(false)
                        .creationDate(Instant.now())
                        .nextPlannedPush(
                                LocalDateTime.from(
                                        LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                                .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                                )
                                        .toInstant(UTC)
                        )
                        .build()
        );

        // When - triggering of the scheduled task

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);

        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);

        assertThat(PsqlManager.findByToken("112233445566"))
                .as(
                        "Check the status of the notification that has been rejected by APNs server - notif is not deactivated"
                )
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastFailurePush())
                                .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isEqualTo("BadMessageId"))
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                        )
                );
    }

}
