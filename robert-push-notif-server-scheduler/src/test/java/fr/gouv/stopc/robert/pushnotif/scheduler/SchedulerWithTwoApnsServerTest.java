package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.ItTools.getRandomNumberInRange;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@ActiveProfiles({ "dev" })
class SchedulerWithTwoApnsServerTest {

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response() {
        // Given
        PsqlManager.givenOnePushInfoSuchAs(
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
        // When -- triggering of the scheduled job

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationSentToSecondaryApnServer = awaitSecondaryAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedBySecondaryApnServer = awaitSecondaryRejectedQueueContainsAtLeast(
                0
        );
        assertThat(notificationSentToMainApnServer).hasSize(1);
        assertThat(notificationSentToSecondaryApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(0);
        assertThat(notificationRejectedBySecondaryApnServer).hasSize(0);

        assertThat(PsqlManager.findByToken("A-TOK1111111111111111"))
                .as("Check the status of the notification that has been correctly sent to main APNs server")
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
                .as("Check the content of the notification received on the main APNs server side")
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
    }

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_rejected_reason_other_than_invalid_token() {
        // Given
        PsqlManager.givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(1L)
                        .token("999999999")
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
        // When -- triggering of the scheduled job

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationSentToSecondaryApnServer = awaitSecondaryAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedBySecondaryApnServer = awaitSecondaryRejectedQueueContainsAtLeast(
                0
        );
        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationSentToSecondaryApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);
        assertThat(notificationRejectedBySecondaryApnServer).hasSize(0);

        assertThat(PsqlManager.findByToken("999999999"))
                .as(
                        "Check the status of the notification that has been rejected by main APNs server (reason other than invalid token)"
                )
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastFailurePush())
                                .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isEqualToIgnoringCase("BadTopic"))
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                        )
                );
    }

    @Test
    void should_send_notification_to_second_apns_server_when_first_replies_invalid_token_response() {

        PsqlManager.givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(4L)
                        .token("123456789")
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
        // When -- triggering of the scheduled job

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationSentToSecondaryApnServer = awaitSecondaryAcceptedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedBySecondaryApnServer = awaitSecondaryRejectedQueueContainsAtLeast(
                0
        );
        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationSentToSecondaryApnServer).hasSize(1);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);
        assertThat(notificationRejectedBySecondaryApnServer).hasSize(0);

        assertThat(PsqlManager.findByToken("123456789"))
                .as("Check the status of the notification that has been correctly sent to secondary APNs server")
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

        assertThat(notificationSentToSecondaryApnServer.get(0))
                .as("Check the content of the notification received on the secondary APNs server side")
                .satisfies(notif -> assertThat(notif.getPushType()).isEqualTo(PushType.BACKGROUND))
                .satisfies(notif -> assertThat(notif.getPriority()).isEqualTo(DeliveryPriority.IMMEDIATE))
                .satisfies(notif -> assertThat(notif.getToken()).isEqualTo("123456789"))
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
    }

    @Test
    void should_deactivate_notification_when_both_server_replies_invalid_token_response() {

        PsqlManager.givenOnePushInfoSuchAs(
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
                                ).toInstant(UTC)
                        )
                        .build()
        );
        // When -- triggering of the scheduled job

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationSentToSecondaryApnServer = awaitSecondaryAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedBySecondaryApnServer = awaitSecondaryRejectedQueueContainsAtLeast(
                1
        );
        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationSentToSecondaryApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);
        assertThat(notificationRejectedBySecondaryApnServer).hasSize(1);

        assertThat(PsqlManager.findByToken("987654321"))
                .as("Check the status of the notification that has been rejected by all APNs server")
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
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                        )
                );

    }

    @Test
    void should_correctly_update_push_status_when_send_notification_to_second_apn_server_with_rejected_reason_other_than_invalid_token() {

        PsqlManager.givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .id(1L)
                        .token("8888888888")
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
        // When -- triggering of the scheduled job

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationSentToSecondaryApnServer = awaitSecondaryAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedBySecondaryApnServer = awaitSecondaryRejectedQueueContainsAtLeast(
                1
        );
        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationSentToSecondaryApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(0);
        assertThat(notificationRejectedBySecondaryApnServer).hasSize(1);

        assertThat(PsqlManager.findByToken("8888888888"))
                .as(
                        "Check the status of the notification that has been rejected by main APNs server (reason other than invalid token)"
                )
                .satisfies(pushInfo -> assertThat(pushInfo.isActive()).isTrue())
                .satisfies(pushInfo -> assertThat(pushInfo.isDeleted()).isFalse())
                .satisfies(pushInfo -> assertThat(pushInfo.getFailedPushSent()).isEqualTo(1))
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getLastFailurePush())
                                .isCloseTo(Instant.now(), within(10, ChronoUnit.SECONDS))
                )
                .satisfies(pushInfo -> assertThat(pushInfo.getLastErrorCode()).isEqualToIgnoringCase("PayloadEmpty"))
                .satisfies(pushInfo -> assertThat(pushInfo.getSuccessfulPushSent()).isEqualTo(0))
                .satisfies(pushInfo -> assertThat(pushInfo.getLastSuccessfulPush()).isNull())
                .satisfies(
                        pushInfo -> assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                        )
                );
    }

}
