package fr.gouv.stopc.robert.pushnotif.scheduler.repository.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoUnit.MINUTES;

@Value
@Builder
public class PushInfo {

    Long id;

    String token;

    String timezone;

    @With
    Instant nextPlannedPush;

    public PushInfo withPushDateTomorrowBetween(final int minPushHour, final int maxPushHour) {
        final var random = ThreadLocalRandom.current();
        final int durationBetweenHours;
        // In case config requires "between 6pm and 4am" which translates in minPushHour
        // = 18 and maxPushHour = 4
        if (maxPushHour < minPushHour) {
            durationBetweenHours = 24 - minPushHour + maxPushHour;
        } else {
            durationBetweenHours = maxPushHour - minPushHour;
        }
        final var nextPushInstant = ZonedDateTime.now(ZoneId.of(timezone)).plusDays(1)
                .withHour((random.nextInt(durationBetweenHours) + minPushHour) % 24)
                .withMinute(random.nextInt(60))
                .toInstant()
                .truncatedTo(MINUTES);

        return this.withNextPlannedPush(nextPushInstant);
    }
}
