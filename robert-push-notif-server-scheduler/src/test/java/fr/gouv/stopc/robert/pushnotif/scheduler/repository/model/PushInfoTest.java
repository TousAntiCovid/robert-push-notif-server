package fr.gouv.stopc.robert.pushnotif.scheduler.repository.model;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class PushInfoTest {

    @RepeatedTest(100)
    void a_random_push_date_for_timezone_GMT0_should_be_between_request_bounds() {
        final var pushInfoUTC = PushInfo.builder()
                .timezone("GMT")
                .build();
        final var nextPush = pushInfoUTC.withPushDateTomorrowBetween(10, 12)
                .getNextPlannedPush()
                .atZone(UTC);
        assertThat("random hour should be between 10 (included) and 12 (excluded)", nextPush.getHour(), oneOf(10, 11));
    }

    @RepeatedTest(100)
    void a_random_push_date_for_timezone_EuropeParis_should_be_between_request_bounds_plus_2() {
        final var pushInfoUTC = PushInfo.builder()
                .timezone("Europe/Paris")
                .build();
        final var nextPush = pushInfoUTC.withPushDateTomorrowBetween(10, 12)
                .getNextPlannedPush()
                .atZone(ZoneId.of("Europe/Paris"));
        assertThat("random hour should be between 10 (included) and 12 (excluded)", nextPush.getHour(), oneOf(10, 11));
    }

    @RepeatedTest(100)
    void can_generate_a_push_date_between_tonight_and_tomorrow_morning() {
        final var pushInfoUTC = PushInfo.builder()
                .timezone("Europe/Paris")
                .build();
        final var nextPush = pushInfoUTC.withPushDateTomorrowBetween(23, 2)
                .getNextPlannedPush()
                .atZone(ZoneId.of("Europe/Paris"));
        assertThat(
                "random hour should be between 23h (included) and 2h (excluded)", nextPush.getHour(), oneOf(23, 0, 1)
        );
    }

    @Test
    void cant_generate_a_meaningful_time_when_min_max_are_equals() {
        final var pushInfoUTC = PushInfo.builder()
                .timezone("Europe/Paris")
                .build();
        assertThatThrownBy(() -> pushInfoUTC.withPushDateTomorrowBetween(10, 10))
                .hasMessage("bound must be positive");
    }
}
