package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.ACCEPTED;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.REJECTED;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.PRIMARY;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.SECONDARY;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.MetricsManager.assertCounterIncremented;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.*;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.awaitility.Awaitility.await;
import static org.exparity.hamcrest.date.InstantMatchers.after;
import static org.exparity.hamcrest.date.InstantMatchers.within;
import static org.hamcrest.Matchers.hasProperty;

@IntegrationTest
@ActiveProfiles({ "test", "one-apns-server" })
@DirtiesContext
class SchedulerNominalTest {

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenPushInfoForTokenAndNextPlannedPush(
                "45f6aa01da5ddb387462c7eaf61bb78ad740f4707bebcf74f9b7c25d48e33589",
                LocalDateTime.from(LocalDate.now().atStartOfDay().plusDays(1)).toInstant(UTC)
        );

        // When - triggering of the scheduled task

        // Then

        // Verify APNs servers
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatNotifsAcceptedBy(PRIMARY)
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue("pushType", PushType.BACKGROUND)
                    .hasFieldOrPropertyWithValue("priority", DeliveryPriority.IMMEDIATE)
                    .hasFieldOrPropertyWithValue(
                            "token",
                            "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad"
                    )
                    .hasFieldOrPropertyWithValue("topic", "test")
                    .hasFieldOrPropertyWithValue("payload", "{\"aps\":{\"badge\":0,\"content-available\":1}}")
                    .is(matching(hasProperty("expiration", within(30, SECONDS, now().plus(1, DAYS)))));

            // Verify Database
            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastFailurePush", null)
                    .hasFieldOrPropertyWithValue("lastErrorCode", null)
                    .hasFieldOrPropertyWithValue("successfulPushSent", 1)
                    .is(matching(hasProperty("lastSuccessfulPush", within(1, MINUTES, now()))))
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));

            assertThatPushInfo("45f6aa01da5ddb387462c7eaf61bb78ad740f4707bebcf74f9b7c25d48e33589")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastFailurePush", null)
                    .hasFieldOrPropertyWithValue("lastErrorCode", null)
                    .hasFieldOrPropertyWithValue("successfulPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastSuccessfulPush", null)
                    .hasFieldOrPropertyWithValue("nextPlannedPush", now().plus(1, DAYS).truncatedTo(DAYS));

            // Verify counters
            assertCounterIncremented(
                    "pushy.notifications.sent.timer",
                    1,
                    Tags.of("outcome", ACCEPTED.name(), "rejectionReason", NONE.name())
            );
        });
    }

    @Test
    void should_deactivate_notification_when_apns_server_replies_with_is_invalid_token_reason() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenApnsServerRejectsTokenIdWith(
                PRIMARY, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_DEVICE_TOKEN
        );

        // When - triggering of the scheduled task

        // Then

        // Verify servers
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatNotifsAcceptedBy(PRIMARY).hasSize(0);
            assertThatNotifsRejectedBy(PRIMARY).hasSize(1);
            assertThatNotifsRejectedBy(SECONDARY).hasSize(0);
            assertThatNotifsRejectedBy(SECONDARY).hasSize(0);

            // Verify database
            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", false)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 1)
                    .is(matching(hasProperty("lastFailurePush", within(1, MINUTES, now()))))
                    .hasFieldOrPropertyWithValue("lastErrorCode", "BadDeviceToken")
                    .hasFieldOrPropertyWithValue("successfulPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastSuccessfulPush", null)
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));

            // Verify counters
            assertCounterIncremented(
                    "pushy.notifications.sent.timer",
                    1,
                    Tags.of("outcome", REJECTED.name(), "rejectionReason", BAD_DEVICE_TOKEN.name())
            );
        });
    }

    @Test
    void should_not_deactivate_notification_when_apns_server_replies_with_no_invalid_token_reason() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenApnsServerRejectsTokenIdWith(
                PRIMARY, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_MESSAGE_ID
        );

        // When - triggering of the scheduled task

        // Then

        // Verify server
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatNotifsAcceptedBy(PRIMARY).hasSize(0);
            assertThatNotifsRejectedBy(PRIMARY).hasSize(1);
            assertThatNotifsAcceptedBy(SECONDARY).hasSize(0);
            assertThatNotifsRejectedBy(SECONDARY).hasSize(0);

            // Verify database
            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 1)
                    .is(matching(hasProperty("lastFailurePush", within(1, MINUTES, now()))))
                    .hasFieldOrPropertyWithValue("lastErrorCode", "BadMessageId")
                    .hasFieldOrPropertyWithValue("successfulPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastSuccessfulPush", null)
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));

            // Verify counters
            assertCounterIncremented(
                    "pushy.notifications.sent.timer",
                    1,
                    Tags.of("outcome", REJECTED.name(), "rejectionReason", BAD_MESSAGE_ID.name())
            );
        });
    }
}
