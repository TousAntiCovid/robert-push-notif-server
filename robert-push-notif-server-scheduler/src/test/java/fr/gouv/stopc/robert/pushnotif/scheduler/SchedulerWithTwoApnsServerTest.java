package fr.gouv.stopc.robert.pushnotif.scheduler;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.FIRST;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.SECOND;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.assertThatPushInfo;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoForToken;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.awaitility.Awaitility.await;
import static org.exparity.hamcrest.date.InstantMatchers.*;
import static org.exparity.hamcrest.date.InstantMatchers.after;
import static org.exparity.hamcrest.date.InstantMatchers.within;
import static org.hamcrest.Matchers.*;

@IntegrationTest
class SchedulerWithTwoApnsServerTest {

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_successful_response() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");

        // When

        // Then
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatMainServerAcceptedOne();
            assertThatMainServerRejectedNothing();
            assertThatSecondServerRejectedNothing();
            assertThatSecondServerAcceptedNothing();

            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastFailurePush", null)
                    .hasFieldOrPropertyWithValue("lastErrorCode", null)
                    .hasFieldOrPropertyWithValue("successfulPushSent", 1)
                    .is(matching(hasProperty("lastSuccessfulPush", within(1, MINUTES, now()))))
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));

            assertThat(getNotifsAcceptedByMainServer())
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue(
                            "token",
                            "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad"
                    )
                    .hasFieldOrPropertyWithValue("pushType", PushType.BACKGROUND)
                    .hasFieldOrPropertyWithValue("priority", DeliveryPriority.IMMEDIATE)
                    .hasFieldOrPropertyWithValue("topic", "test")
                    .hasFieldOrPropertyWithValue("payload", "{\"aps\":{\"badge\":0,\"content-available\":1}}")
                    .satisfies(
                            notif -> assertThat(notif.getExpiration())
                                    .isCloseTo(now().plus(1, DAYS), within(30, ChronoUnit.SECONDS))
                    );
        });
    }

    @Test
    void should_correctly_update_push_status_when_send_notification_to_first_apn_server_with_rejected_reason_other_than_invalid_token() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenApnsServerRejectsTokenIdWith(
                FIRST, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_TOPIC
        );

        // When -- triggering of the scheduled job

        // Then
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatMainServerAcceptedNothing();
            assertThatMainServerRejectedOne();
            assertThatSecondServerAcceptedNothing();
            assertThatSecondServerRejectedNothing();

            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 1)
                    .is(matching(hasProperty("lastFailurePush", within(1, MINUTES, now()))))
                    .hasFieldOrPropertyWithValue("lastErrorCode", "BadTopic")
                    .hasFieldOrPropertyWithValue("successfulPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastSuccessfulPush", null)
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));
        });
    }

    @Test
    void should_send_notification_to_second_apns_server_when_first_replies_invalid_token_response() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenApnsServerRejectsTokenIdWith(
                FIRST, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_DEVICE_TOKEN
        );

        // When -- triggering of the scheduled job

        // Then
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatMainServerAcceptedNothing();
            assertThatMainServerRejectedOne();
            assertThatSecondServerAcceptedOne();
            assertThatSecondServerRejectedNothing();

            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastFailurePush", null)
                    .hasFieldOrPropertyWithValue("lastErrorCode", null)
                    .hasFieldOrPropertyWithValue("successfulPushSent", 1)
                    .is(matching(hasProperty("lastSuccessfulPush", within(1, MINUTES, now()))))
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));

            assertThat(getNotifsAcceptedBySecondServer())
                    .hasSize(1)
                    .first()
                    .hasFieldOrPropertyWithValue(
                            "token",
                            "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad"
                    )
                    .hasFieldOrPropertyWithValue("pushType", PushType.BACKGROUND)
                    .hasFieldOrPropertyWithValue("priority", DeliveryPriority.IMMEDIATE)
                    .hasFieldOrPropertyWithValue("topic", "test")
                    .hasFieldOrPropertyWithValue("payload", "{\"aps\":{\"badge\":0,\"content-available\":1}}")
                    .satisfies(
                            notif -> assertThat(notif.getExpiration())
                                    .isCloseTo(now().plus(1, DAYS), within(30, ChronoUnit.SECONDS))
                    );
        });
    }

    @Test
    void should_deactivate_notification_when_both_server_replies_invalid_token_response() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenApnsServerRejectsTokenIdWith(
                FIRST, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_DEVICE_TOKEN
        );
        givenApnsServerRejectsTokenIdWith(
                SECOND, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_DEVICE_TOKEN
        );

        // When -- triggering of the scheduled job

        // Then

        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {

            assertThatMainServerAcceptedNothing();
            assertThatMainServerRejectedOne();
            assertThatSecondServerAcceptedNothing();
            assertThatSecondServerRejectedOne();

            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", false)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 1)
                    .is(matching(hasProperty("lastFailurePush", within(1, MINUTES, now()))))
                    .hasFieldOrPropertyWithValue("lastErrorCode", "BadDeviceToken")
                    .hasFieldOrPropertyWithValue("successfulPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastSuccessfulPush", null)
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));
        });
    }

    @Test
    void should_correctly_update_push_status_when_send_notification_to_second_apn_server_with_rejected_reason_other_than_invalid_token() {

        // Given
        givenPushInfoForToken("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad");
        givenApnsServerRejectsTokenIdWith(
                FIRST, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", BAD_DEVICE_TOKEN
        );
        givenApnsServerRejectsTokenIdWith(
                SECOND, "740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad", PAYLOAD_EMPTY
        );

        // When -- triggering of the scheduled job

        // Then
        await().atMost(40, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThatMainServerAcceptedNothing();
            assertThatMainServerRejectedOne();
            assertThatSecondServerAcceptedNothing();
            assertThatSecondServerRejectedOne();

            assertThatPushInfo("740f4707bebcf74f9b7c25d48e3358945f6aa01da5ddb387462c7eaf61bb78ad")
                    .hasFieldOrPropertyWithValue("active", true)
                    .hasFieldOrPropertyWithValue("deleted", false)
                    .hasFieldOrPropertyWithValue("failedPushSent", 1)
                    .is(matching(hasProperty("lastFailurePush", within(1, MINUTES, now()))))
                    .hasFieldOrPropertyWithValue("lastErrorCode", "PayloadEmpty")
                    .hasFieldOrPropertyWithValue("successfulPushSent", 0)
                    .hasFieldOrPropertyWithValue("lastSuccessfulPush", null)
                    .is(matching(hasProperty("nextPlannedPush", after(now().plus(1, DAYS).truncatedTo(DAYS)))));
        });
    }
}
