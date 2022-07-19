package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.FIRST;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.SECOND;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoWith;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@ActiveProfiles({ "dev" })
class SchedulerWithTwoApnsServerTest {

    @Autowired
    Scheduler scheduler;

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response() {

        // Given
        givenPushInfoWith(b -> b.id(1L).token("token"));

        // When
        scheduler.sendNotifications();

        // Then
        assertThatMainServerAcceptedOne();
        assertThatMainServerRejectedNothing();
        assertThatSecondServerRejectedNothing();
        assertThatSecondServerAcceptedNothing();

        assertThat(PsqlManager.findByToken("token"))
                .as("Check the status of the notification that has been correctly sent to main APNs server")
                .satisfies(
                        p -> {
                            assertThat(p.getLastSuccessfulPush())
                                    .as("Last successful push should have been updated")
                                    .isNotNull();
                            assertThat(p.getNextPlannedPush()).isAfter(
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

        assertThat(getNotifsAcceptedByMainServer().get(0))
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
        givenPushInfoWith(b -> b.id(1L).token("token"));
        givenApnsServerRejectsTokenIdWith(FIRST, "token", BAD_TOPIC);

        // When -- triggering of the scheduled job
        scheduler.sendNotifications();

        // Then
        assertThatMainServerAcceptedNothing();
        assertThatMainServerRejectedOne();
        assertThatSecondServerAcceptedNothing();
        assertThatSecondServerRejectedNothing();

        assertThat(PsqlManager.findByToken("token"))
                .as(
                        "Check the status of the notification that has been rejected by main APNs server (reason other than invalid token)"
                )
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush())
                                    .as("Last failure push should have been updated")
                                    .isNotNull();
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

        // Given
        givenPushInfoWith(b -> b.id(4L).token("token"));
        givenApnsServerRejectsTokenIdWith(FIRST, "token", BAD_DEVICE_TOKEN);

        // When -- triggering of the scheduled job
        scheduler.sendNotifications();

        // Then
        assertThatMainServerAcceptedNothing();
        assertThatMainServerRejectedOne();
        assertThatSecondServerAcceptedOne();
        assertThatSecondServerRejectedNothing();

        assertThat(PsqlManager.findByToken("token"))
                .as("Check the status of the notification that has been correctly sent to secondary APNs server")
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastSuccessfulPush())
                                    .as("Last successful push should have been updated")
                                    .isNotNull();
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

        assertThat(getNotifsAcceptedBySecondServer().get(0))
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
                        PushType.BACKGROUND, DeliveryPriority.IMMEDIATE, "token", "test",
                        "{\"aps\":{\"badge\":0,\"content-available\":1}}"
                );
    }

    @Test
    void should_deactivate_notification_when_both_server_replies_invalid_token_response() {

        // Given
        givenPushInfoWith(b -> b.id(3L).token("token"));
        givenApnsServerRejectsTokenIdWith(FIRST, "token", BAD_DEVICE_TOKEN);
        givenApnsServerRejectsTokenIdWith(SECOND, "token", BAD_DEVICE_TOKEN);

        // When -- triggering of the scheduled job
        scheduler.sendNotifications();

        // Then
        assertThatMainServerAcceptedNothing();
        assertThatMainServerRejectedOne();
        assertThatSecondServerAcceptedNothing();
        assertThatSecondServerRejectedOne();

        assertThat(PsqlManager.findByToken("token"))
                .as("Check the status of the notification that has been rejected by all APNs server")
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush())
                                    .as("Last failure push should have been updated")
                                    .isNotNull();
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

        // Given
        givenPushInfoWith(b -> b.id(1L).token("token"));
        givenApnsServerRejectsTokenIdWith(FIRST, "token", BAD_DEVICE_TOKEN);
        givenApnsServerRejectsTokenIdWith(SECOND, "token", PAYLOAD_EMPTY);

        // When -- triggering of the scheduled job
        scheduler.sendNotifications();

        // Then
        assertThatMainServerAcceptedNothing();
        assertThatMainServerRejectedOne();
        assertThatSecondServerAcceptedNothing();
        assertThatSecondServerRejectedOne();

        assertThat(PsqlManager.findByToken("token"))
                .as(
                        "Check the status of the notification that has been rejected by main APNs server (reason other than invalid token)"
                )
                .satisfies(
                        pushInfo -> {
                            assertThat(pushInfo.getLastFailurePush())
                                    .as("Last failure push should have been updated")
                                    .isNotNull();
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
