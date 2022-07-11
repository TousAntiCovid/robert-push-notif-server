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
import java.util.List;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitMainRejectedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitSecondaryAcceptedQueueContainsAtLeast;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsServersManager.awaitSecondaryRejectedQueueContainsAtLeast;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@ActiveProfiles({ "dev" })
class SchedulerWithTwoApnsServerTest {

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response() {
        // Given
        PsqlManager.givenPushInfoWith(b -> b.id(1L).token("A-TOK1111111111111111")
        // .nextPlannedPush(
        // LocalDateTime.from(
        // LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
        // .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
        // ).toInstant(UTC)
        // )
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
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastSuccessfulPush())
                                    .isCloseTo(Instant.now(), within(10, SECONDS));
                            assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                    LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                            );
                        }
                )
                .extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastFailurePush,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent
                )
                .containsExactly(true, false, 0, null, null, 1);
        ;

        assertThat(notificationSentToMainApnServer.get(0))
                .as("Check the content of the notification received on the main APNs server side")
                .satisfies(
                        notif -> assertThat(notif.getExpiration())
                                .isCloseTo(Instant.now().plus(Duration.ofDays(1)), within(30, SECONDS))
                ).extracting(
                        ApnsPushNotification::getPushType,
                        ApnsPushNotification::getPriority,
                        ApnsPushNotification::getToken,
                        ApnsPushNotification::getTopic,
                        ApnsPushNotification::getPayload
                ).containsExactly(
                        PushType.BACKGROUND, DeliveryPriority.IMMEDIATE, "a1111111111111111", "test",
                        "{\"aps\":{\"badge\":0,\"content-available\":1}}"
                );
    }

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_rejected_reason_other_than_invalid_token() {
        // Given
        PsqlManager.givenPushInfoWith(b -> b.id(1L).token("999999999")
        // .nextPlannedPush(
        // LocalDateTime.from(
        // LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
        // .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
        // ).toInstant(UTC)
        // )
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
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush())
                                    .isCloseTo(Instant.now(), within(10, SECONDS));
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
                .containsExactly(true, false, 1, "BadTopic", 0, null);
    }

    @Test
    void should_send_notification_to_second_apns_server_when_first_replies_invalid_token_response() {

        PsqlManager.givenPushInfoWith(b -> b.id(4L).token("123456789")
        // .nextPlannedPush(
        // LocalDateTime.from(
        // LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
        // .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
        // ).toInstant(UTC)
        // )
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
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastSuccessfulPush())
                                    .isCloseTo(Instant.now(), within(10, SECONDS));
                            assertThat(pushInfo.getNextPlannedPush())
                                    .isAfter(
                                            LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1))
                                                    .toInstant(UTC)
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

        assertThat(notificationSentToSecondaryApnServer.get(0))
                .as("Check the content of the notification received on the secondary APNs server side")
                .satisfies(
                        notif -> assertThat(notif.getExpiration())
                                .isCloseTo(Instant.now().plus(Duration.ofDays(1)), within(30, SECONDS))
                ).extracting(
                        ApnsPushNotification::getPushType,
                        ApnsPushNotification::getPriority,
                        ApnsPushNotification::getToken,
                        ApnsPushNotification::getTopic,
                        ApnsPushNotification::getPayload
                )
                .containsExactly(
                        PushType.BACKGROUND, DeliveryPriority.IMMEDIATE, "123456789", "test",
                        "{\"aps\":{\"badge\":0,\"content-available\":1}}"
                );
    }

    @Test
    void should_deactivate_notification_when_both_server_replies_invalid_token_response() {

        PsqlManager.givenPushInfoWith(b -> b.id(3L).token("987654321")
        // .nextPlannedPush(
        // LocalDateTime.from(
        // LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
        // .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
        // ).toInstant(UTC)
        // )
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
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush())
                                    .isCloseTo(Instant.now(), within(10, SECONDS));
                            assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                    LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                            );
                        }
                )
                .extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getLastSuccessfulPush
                )
                .containsExactly(false, false, 1, "BadDeviceToken", 0, null);

    }

    @Test
    void should_correctly_update_push_status_when_send_notification_to_second_apn_server_with_rejected_reason_other_than_invalid_token() {

        PsqlManager.givenPushInfoWith(b -> b.id(1L).token("8888888888")
        // .nextPlannedPush(
        // LocalDateTime.from(
        // LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
        // .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
        // ).toInstant(UTC)
        // )
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
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush())
                                    .isCloseTo(Instant.now(), within(10, SECONDS));
                            assertThat(pushInfo.getNextPlannedPush()).isAfter(
                                    LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                            );
                        }
                )
                .extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getLastSuccessfulPush
                )
                .containsExactly(true, false, 1, "PayloadEmpty", 0, null);
    }
}
