package fr.gouv.stopc.robert.pushnotif.scheduler.ratelimiting;

import fr.gouv.stopc.robert.pushnotif.scheduler.Scheduler;
import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.assertThatAllPushInfo;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoForToken;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.LongStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
@TestPropertySource(properties = {
        "robert.push.server.max-notifications-per-second=2",
        "robert.push.server.scheduler.delay-in-ms=10000000000"
})
class SchedulerRateLimiting2sTest {

    @Autowired
    Scheduler scheduler;

    @ParameterizedTest
    @ValueSource(ints = { 10, 25, 50 })
    void should_send_notificationsNumber_notifs_in_at_least_notificationsNumber_seconds(final int notificationsNumber) {

        // Given
        rangeClosed(1, notificationsNumber)
                .forEach(i -> givenPushInfoForToken(randomUUID().toString()));

        // When
        final var before = now();
        scheduler.sendNotifications();
        final var after = now();

        // Then
        assertThatAllPushInfo()
                .hasSize(notificationsNumber)
                .extracting(
                        PushInfo::isActive,
                        PushInfo::isDeleted,
                        PushInfo::getFailedPushSent,
                        PushInfo::getLastFailurePush,
                        PushInfo::getLastErrorCode,
                        PushInfo::getSuccessfulPushSent,
                        PushInfo::getFailedPushSent
                )
                .containsOnly(tuple(true, false, 0, null, null, 1, 0));

        final var expectedDuration = Duration.ofSeconds(notificationsNumber / 2);
        assertThat(Duration.between(before, after))
                .isBetween(expectedDuration.minusSeconds(1), expectedDuration.plusSeconds(1));
    }
}
