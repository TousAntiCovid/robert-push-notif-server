package fr.gouv.stopc.robert.pushnotif.scheduler.data;

import lombok.experimental.UtilityClass;

import java.sql.Timestamp;
import java.time.Instant;

@UtilityClass
public class InstantTimestampConverter {

    static Instant convertTimestampToInstant(final Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    public static Timestamp convertInstantToTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

}
