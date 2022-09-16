package fr.gouv.stopc.robert.pushnotif.scheduler;

import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.PushInfo;
import org.junit.jupiter.api.Test;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.assertThatAllPushInfo;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoForToken;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.LongStream.rangeClosed;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class SchedulerVolumetryTest {

    private static final int PUSH_NOTIF_COUNT = 100;

    // This test class is useful to do test with volumetry
    @Test
    void should_correctly_send_large_amount_of_notification_to_apns_servers() {

        // Given
        rangeClosed(1, PUSH_NOTIF_COUNT).forEach(i -> givenPushInfoForToken(randomUUID().toString()));

        // When -- triggering of the scheduled job

        // Then
        await().atMost(40, SECONDS).untilAsserted(() -> {
            assertThatMainServerAccepted(PUSH_NOTIF_COUNT);
            assertThatSecondServerAcceptedNothing();
            assertThatSecondServerRejectedNothing();

            assertThatAllPushInfo()
                    .hasSize(PUSH_NOTIF_COUNT)
                    .extracting(
                            PushInfo::isActive,
                            PushInfo::isDeleted,
                            PushInfo::getSuccessfulPushSent,
                            PushInfo::getFailedPushSent,
                            PushInfo::getLastFailurePush,
                            PushInfo::getLastErrorCode
                    )
                    .containsOnly(tuple(true, false, 1, 0, null, null));
        });
    }
}
