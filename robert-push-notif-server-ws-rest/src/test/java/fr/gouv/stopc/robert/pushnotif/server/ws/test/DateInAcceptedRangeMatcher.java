package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.ZonedDateTime;
import java.util.Date;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.Matchers.is;

public class DateInAcceptedRangeMatcher extends TypeSafeDiagnosingMatcher<Date> {

    public static Matcher<Date> isLocalTimeBetween8amAnd7pm() {
        return is(new DateInAcceptedRangeMatcher());
    }

    Date tomorrow8am = Date.from(
            ZonedDateTime.now().plusDays(1).withHour(8).truncatedTo(HOURS).toInstant()
    );

    Date tomorrow8pm = Date.from(
            ZonedDateTime.now().plusDays(1).withHour(20).truncatedTo(HOURS).toInstant()
    );

    @Override
    public void describeTo(Description description) {
        description.appendText("A date between " + tomorrow8am + " and " + tomorrow8pm);
    }

    @Override
    protected boolean matchesSafely(Date date, Description mismatchDescription) {
        final var validLowerBound = date.equals(tomorrow8am) || date.after(tomorrow8am);
        final var validUpperBound = date.equals(tomorrow8pm) || date.before(tomorrow8pm);
        if (!validLowerBound) {
            mismatchDescription.appendText("was a value below expected bound ").appendValue(date);
        }
        if (!validUpperBound) {
            mismatchDescription.appendText("was a value above expected bound").appendValue(date);
        }
        return validLowerBound && validUpperBound;

    }
}
