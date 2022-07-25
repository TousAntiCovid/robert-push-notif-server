package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.is;

public class InstantInAcceptedRangeMatcher extends TypeSafeDiagnosingMatcher<Instant> {

    public static Matcher<Instant> isTimeBetween8amAnd7Pm(String timezone) {
        return is(new InstantInAcceptedRangeMatcher(timezone));
    }

    private final ZoneId zoneId;

    private final Instant lowerBound;

    private final Instant upperBound;

    private InstantInAcceptedRangeMatcher(final String timezone) {
        zoneId = ZoneId.of(timezone);
        lowerBound = ZonedDateTime.now(zoneId)
                .plusDays(1)
                .withHour(8)
                .truncatedTo(HOURS)
                .toInstant();
        upperBound = ZonedDateTime.now(zoneId)
                .plusDays(1)
                .withHour(20)
                .truncatedTo(HOURS)
                .toInstant();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("An instant between tomorrow 8:00 and tomorrow 20:00 at zone " + zoneId);
    }

    @Override
    protected boolean matchesSafely(Instant instant, Description mismatchDescription) {

        final var validLowerBound = instant.equals(lowerBound) || instant.isAfter(lowerBound);
        final var validUpperBound = instant.equals(upperBound) || instant.isBefore(upperBound);
        if (!validLowerBound) {
            mismatchDescription.appendText("was a value below expected bound ").appendValue(instant);
        }
        if (!validUpperBound) {
            mismatchDescription.appendText("was a value above expected bound").appendValue(instant);
        }
        return validLowerBound && validUpperBound;

    }
}
