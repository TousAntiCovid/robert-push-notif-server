package fr.gouv.stopc.robert.pushnotif.scheduler;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static fr.gouv.stopc.robert.pushnotif.scheduler.Scheduler.generatePushDateTomorrowBetween;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.oneOf;

class SchedulerRandomDateGenerationTest {

    @RepeatedTest(100)
    void a_random_push_date_for_timezone_GMT0_should_be_between_request_bounds() {
        final var nextPush = generatePushDateTomorrowBetween(10, 12, ZoneId.of("GMT"))
                .atZone(UTC);
        assertThat("random hour should be between 10 (included) and 12 (excluded)", nextPush.getHour(), oneOf(10, 11));
    }

    @RepeatedTest(100)
    void a_random_push_date_for_timezone_EuropeParis_should_be_between_request_bounds_plus_2() {
        final var nextPush = generatePushDateTomorrowBetween(10, 12, ZoneId.of("Europe/Paris"))
                .atZone(ZoneId.of("Europe/Paris"));
        assertThat("random hour should be between 10 (included) and 12 (excluded)", nextPush.getHour(), oneOf(10, 11));
    }

    @RepeatedTest(100)
    void can_generate_a_push_date_between_tonight_and_tomorrow_morning() {
        final var nextPush = generatePushDateTomorrowBetween(23, 2, ZoneId.of("Europe/Paris"))
                .atZone(ZoneId.of("Europe/Paris"));
        assertThat(
                "random hour should be between 23h (included) and 2h (excluded)", nextPush.getHour(), oneOf(23, 0, 1)
        );
    }

    @Test
    void cant_generate_a_meaningful_time_when_min_max_are_equals() {
        assertThatThrownBy(() -> generatePushDateTomorrowBetween(10, 10, ZoneId.of("Europe/Paris")))
                .hasMessage("bound must be positive");
    }
}
