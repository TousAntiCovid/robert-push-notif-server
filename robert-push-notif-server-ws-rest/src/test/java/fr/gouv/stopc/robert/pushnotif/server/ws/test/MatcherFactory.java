package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.hamcrest.Matcher;

import java.util.Date;

import static org.hamcrest.Matchers.is;

public class MatcherFactory {

    public static Matcher<Date> isLocalTimeBetween8amAnd7pm() {
        return is(new DateInAcceptedRangeMatcher());
    }

}
