package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.ZonedDateTime;
import java.util.Date;

import static org.hamcrest.Matchers.is;

public class DateInAcceptedRangeMatcher extends TypeSafeDiagnosingMatcher<Date> {

    public static Matcher<Date> isLocalTimeBetween8amAnd7pm() {
        return is(new DateInAcceptedRangeMatcher());
    }

    Date tomorrow8am = Date.from(
            ZonedDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0).toInstant()
    );

    Date tomorrow7pm = Date.from(
            ZonedDateTime.now().plusDays(1).withHour(19).withMinute(0).withSecond(0).toInstant()
    );

    @Override
    public void describeTo(Description description) {
        description.appendText("A date between " + tomorrow8am + " and " + tomorrow7pm);
    }

    @Override
    protected boolean matchesSafely(Date date, Description mismatchDescription) {
        boolean test = date.after(tomorrow8am) && date.before(tomorrow7pm);
        if (!test) {
            mismatchDescription.appendText("was a date equal to " + date);
        }
        return test;

    }
}
