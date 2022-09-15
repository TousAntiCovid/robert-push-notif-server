package fr.gouv.stopc.robert.pushnotif.scheduler.repository.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoUnit.MINUTES;

@Data
@Builder
public class PushInfo {

    private Long id;

    private String token;

    private String timezone;

    private String locale;

    private Instant nextPlannedPush;

    private Instant lastSuccessfulPush;

    private Instant lastFailurePush;

    private String lastErrorCode;

    private int successfulPushSent;

    private int failedPushSent;

    private Instant creationDate;

    private boolean active;

    private boolean deleted;

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

        setNextPlannedPush(nextPushInstant);
        return this;
    }
}
