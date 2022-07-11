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
import java.util.List;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainRejectedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoWith;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
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
        givenPushInfoWith(b -> b.id(1L).token("A-TOK1111111111111111"));
        givenPushInfoWith(b -> b.id(2L).token("FUTURE-1111111111111112").nextPlannedPush(TOMORROW_PLANNED_DATE));

        // When - triggering of the scheduled task

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(1);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(0);

        assertThat(notificationSentToMainApnServer).hasSize(1);
        assertThat(notificationRejectedByMainApnServer).hasSize(0);

        assertThat(PsqlManager.findByToken("A-TOK1111111111111111"))
                .as("Check the status of the notification that has been correctly sent to APNs server")
                .satisfies(pushInfo -> {
                    assertThat(pushInfo.getLastSuccessfulPush())
                            .isCloseTo(Instant.now(), within(10, SECONDS));
                    assertThat(pushInfo.getNextPlannedPush()).isAfter(
                            LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                    );
                }
                ).extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastFailurePush,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent
                )
                .containsExactly(true, false, 0, null, null, 1);

        assertThat(notificationSentToMainApnServer.get(0))
                .as("Check the content of the notification received on the APNs server side")
                .satisfies(
                        notif -> {
                            assertThat(notif.getExpiration())
                                    .isCloseTo(Instant.now().plus(Duration.ofDays(1)), within(30, SECONDS));
                            assertThat(notif.getPayload())
                                    .isEqualTo("{\"aps\":{\"badge\":0,\"content-available\":1}}");
                        }
                ).extracting(
                        ApnsPushNotification::getPushType,
                        ApnsPushNotification::getPriority,
                        ApnsPushNotification::getToken,
                        ApnsPushNotification::getTopic
                )
                .containsExactly(PushType.BACKGROUND, DeliveryPriority.IMMEDIATE, "a1111111111111111", "test");

        assertThat(PsqlManager.findByToken("FUTURE-1111111111111112"))
                .as("This notification is not pushed because its planned date is in future")
                .extracting(
                        PushInfo::isActive, PushInfo::isDeleted, PushInfo::getFailedPushSent,
                        PushInfo::getLastFailurePush,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getLastSuccessfulPush,
                        PushInfo::getNextPlannedPush
                )
                .containsExactly(true, false, 0, null, null, 0, null, TOMORROW_PLANNED_DATE);
    }

    @Test
    void should_deactivate_notification_when_apns_server_replies_with_is_invalid_token_reason() {

        // Given
        givenPushInfoWith(b -> b.id(3L).token("987654321"));

        // When - triggering of the scheduled task

        // Then
        List<ApnsPushNotification> notificationSentToMainApnServer = awaitMainAcceptedQueueContainsAtLeast(0);
        List<ApnsPushNotification> notificationRejectedByMainApnServer = awaitMainRejectedQueueContainsAtLeast(1);

        assertThat(notificationSentToMainApnServer).hasSize(0);
        assertThat(notificationRejectedByMainApnServer).hasSize(1);

        assertThat(PsqlManager.findByToken("987654321"))
                .as("Check the status of the notification that has been rejected by APNs server - notif is deactivated")
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush()).isCloseTo(Instant.now(), within(10, SECONDS));
                            assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                    LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1))
                                            .toInstant(UTC)
                            );
                        }
                ).extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getLastSuccessfulPush
                )
                .contains(false, false, 1, "BadDeviceToken", 0, null);
    }

    @Test
    void should_not_deactivate_notification_when_apns_server_replies_with_no_invalid_token_reason() {
        // Given
        givenPushInfoWith(b -> b.id(4L).token("112233445566"));

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
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush()).isCloseTo(Instant.now(), within(10, SECONDS));
                            assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                    LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                            );
                        }
                ).extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getLastSuccessfulPush
                )
                .contains(true, false, 1, "BadMessageId", 0, null);
    }

}
