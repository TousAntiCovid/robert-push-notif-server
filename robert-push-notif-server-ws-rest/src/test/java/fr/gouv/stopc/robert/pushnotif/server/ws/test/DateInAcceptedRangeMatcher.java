package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.is;

public class DateInAcceptedRangeMatcher extends TypeSafeDiagnosingMatcher<Date> {

    public static Matcher<Date> isTimeBetween8amAnd7Pm(String timezone) {
        return is(new DateInAcceptedRangeMatcher(timezone));
    }

    private final ZoneId zoneId;

    private final Instant lowerBound;

    private final Instant upperBound;

    private DateInAcceptedRangeMatcher(String timezone) {
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
        description.appendText("A datetime between tomorrow 8:00 and tomorrow 20:00 at zone " + zoneId);
    }

    @Override
    protected boolean matchesSafely(Date date, Description mismatchDescription) {
        final var actualInstant = date.toInstant();

        final var validLowerBound = actualInstant.equals(lowerBound) || actualInstant.isAfter(lowerBound);
        final var validUpperBound = actualInstant.equals(upperBound) || actualInstant.isBefore(upperBound);
        if (!validLowerBound) {
            mismatchDescription.appendText("was a value below expected bound ").appendValue(actualInstant);
        }
        if (!validUpperBound) {
            mismatchDescription.appendText("was a value above expected bound").appendValue(actualInstant);
        }
        return validLowerBound && validUpperBound;

    }
}
