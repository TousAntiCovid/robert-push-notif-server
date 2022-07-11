// package fr.gouv.stopc.robert.pushnotif.scheduler;
//
// import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
// import fr.gouv.stopc.robert.pushnotif.scheduler.test.IntegrationTest;
// import fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.test.context.ActiveProfiles;
//
// import java.time.Duration;
// import java.time.Instant;
//
// import static
// fr.gouv.stopc.robert.pushnotif.scheduler.test.PsqlManager.givenPushInfoWith;
// import static java.time.Instant.now;
// import static java.util.UUID.randomUUID;
// import static java.util.stream.LongStream.rangeClosed;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.tuple;
//
//// @IntegrationTest
//// @ActiveProfiles({ "dev", "rate-limiting" })
// public class SchedulerRateLimitingTest {
//
//// @Autowired
//// Scheduler scheduler;
//
//// @Test
// void
// should_send_50_notifs_in_at_least_50_seconds_when_limited_to_1_notif_per_second()
// {
//
// // Given
// rangeClosed(1, 50).forEach(i -> givenPushInfoWith(p ->
// p.id(i).token(randomUUID().toString())));
//
// // when
// final var before = now();
// scheduler.sendNotifications();
// Instant after = now();
//
// assertThat(PsqlManager.findAll()).hasSize(50)
// .extracting(
// PushInfo::isActive,
// PushInfo::isDeleted,
// PushInfo::getFailedPushSent,
// PushInfo::getLastFailurePush,
// PushInfo::getLastErrorCode,
// PushInfo::getSuccessfulPushSent,
// PushInfo::getFailedPushSent
// )
// .containsOnly(tuple(true, false, 0, null, null, 1, 0));
//
// assertThat(Duration.between(before,
// after)).isGreaterThanOrEqualTo(Duration.ofSeconds(50));
// }
// }
