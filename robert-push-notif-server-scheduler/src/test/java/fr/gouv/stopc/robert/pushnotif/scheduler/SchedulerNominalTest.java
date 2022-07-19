package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.ACCEPTED;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.REJECTED;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.FIRST;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.MetricsManager.assertCounterIncremented;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoWith;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@IntegrationTest
@ActiveProfiles({ "dev", "one-apns-server" })
@DirtiesContext
class SchedulerNominalTest {

    @Autowired
    Scheduler scheduler;

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response()
            throws InterruptedException {

        // Given
        givenPushInfoWith(b -> b.id(1L).token("A-TOK1111111111111111"));
        givenPushInfoWith(
                b -> b.id(2L)
                        .token("FUTURE-1111111111111112")
                        .nextPlannedPush(LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC))
        );

        // When - triggering of the scheduled task
        scheduler.sendNotifications();

        // Then

        // Verify servers
        assertThatMainServerAcceptedOne();
        assertThatMainServerRejectedNothing();
        assertThat(APNsMockServersManager.getNotifsAcceptedByMainServer().get(0))
                .as("Check the content of the notification received on the APNs server side")
                .satisfies(
                        notif -> {
                            assertThat(notif.getExpiration())
                                    .isCloseTo(now().plus(Duration.ofDays(1)), within(30, SECONDS));
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

        // Verify Database
        assertThat(PsqlManager.findByToken("A-TOK1111111111111111"))
                .as("Check the status of the notification that has been correctly sent to APNs server")
                .satisfies(pushInfo -> {
                    assertThat(pushInfo.getLastSuccessfulPush())
                            .as("Last successful push should have been updated")
                            .isNotNull();
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
                .containsExactly(
                        true, false, 0, null, null, 0, null, LocalDateTime
                                .from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
                );
        // Verify counters
        assertCounterIncremented(
                "pushy.notifications.sent.timer",
                1,
                Tags.of("outcome", ACCEPTED.name(), "rejectionReason", NONE.name())
        );
    }

    @Test
    void should_deactivate_notification_when_apns_server_replies_with_is_invalid_token_reason() {

        // Given
        givenPushInfoWith(b -> b.id(3L).token("A-TOK1111111111111111"));
        givenApnsServerRejectsTokenIdWith(FIRST, "a1111111111111111", BAD_DEVICE_TOKEN);

        // When - triggering of the scheduled task
        scheduler.sendNotifications();

        // Then

        // Verify servers
        assertThatMainServerAcceptedNothing();
        assertThatMainServerRejectedOne();

        // Verify database
        assertThat(PsqlManager.findByToken("A-TOK1111111111111111"))
                .as("Check the status of the notification that has been rejected by APNs server - notif is deactivated")
                .satisfies(
                        pushInfoFromBase -> {
                            assertThat(pushInfoFromBase.getLastFailurePush())
                                    .as("Last successful push should have been updated")
                                    .isNotNull();
                            assertThat(pushInfoFromBase.getNextPlannedPush()).isAfter(
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

        // Verify counters
        assertCounterIncremented(
                "pushy.notifications.sent.timer",
                1,
                Tags.of("outcome", REJECTED.name(), "rejectionReason", BAD_DEVICE_TOKEN.name())
        );
    }

    @Test
    void should_not_deactivate_notification_when_apns_server_replies_with_no_invalid_token_reason() {

        // Given
        givenPushInfoWith(b -> b.id(4L).token("A-TOK1111111111111111"));
        givenApnsServerRejectsTokenIdWith(FIRST, "a1111111111111111", BAD_MESSAGE_ID);

        // When - triggering of the scheduled task
        scheduler.sendNotifications();

        // Then

        // Verify server
        assertThatMainServerAcceptedNothing();
        assertThatMainServerRejectedOne();

        // Verify database
        assertThat(PsqlManager.findByToken("A-TOK1111111111111111"))
                .as(
                        "Check the status of the notification that has been rejected by APNs server - notif is not deactivated"
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
                .contains(true, false, 1, "BadMessageId", 0, null);

        // Verify counters
        assertCounterIncremented(
                "pushy.notifications.sent.timer",
                1,
                Tags.of("outcome", REJECTED.name(), "rejectionReason", BAD_MESSAGE_ID.name())
        );
    }
}
