package fr.gouv.stopc.robert.pushnotif.scheduler;

import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoWith;
import static java.util.UUID.randomUUID;
import static java.util.stream.LongStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
@ActiveProfiles({ "dev" })
class SchedulerVolumetryTest {

    private static final int PUSH_NOTIF_COUNT = 5000;

    @Autowired
    Scheduler scheduler;

    // This test class is useful to do test with volumetry
    // @Disabled
    @Test
    void should_correctly_send_large_amount_of_notification_to_apns_servers() {

        // Given
        rangeClosed(1, PUSH_NOTIF_COUNT).forEach(i -> givenPushInfoWith(b -> b.id(i).token(randomUUID().toString())));

        // When
        scheduler.sendNotifications();

        // Then
        assertThatMainServerAccepted(PUSH_NOTIF_COUNT);
        assertThatSecondServerAcceptedNothing();
        assertThatSecondServerRejectedNothing();

        assertThat(PsqlManager.findAll()).hasSize(PUSH_NOTIF_COUNT)
                .extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastFailurePush,
                        PushInfo::getLastErrorCode
                )
                .containsOnly(tuple(true, false, 1, 0, null, null));
    }
}
