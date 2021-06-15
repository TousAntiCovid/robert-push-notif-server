package fr.gouv.stopc.robert.pushnotif.common.utils;

import static java.time.temporal.ChronoUnit.HOURS;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TimeUtils {

    public final static String UTC = "UTC";

    private static Random random = new Random();

    private TimeUtils() {
        throw new AssertionError();
    }

    private static Optional<LocalDateTime> toDateAtTimezone(PushDate pushdate) {

        return Optional.ofNullable(Instant.ofEpochMilli(pushdate.getLastPushDate().getTime())
                .atZone(ZoneId.of(UTC))
                .withZoneSameInstant(ZoneId.of(pushdate.getTimezone()))
                .toLocalDateTime());
    }

    private static Optional<Date> toDateTimezoneUTC(LocalDateTime dateTime, String currentTimezone) {

        return Optional.ofNullable(Date.from(dateTime.atZone(ZoneId.of(currentTimezone))
                .withZoneSameInstant(ZoneId.of(UTC)).toInstant()));
    }

    private static boolean isValidHour(PushDate pushDate) {
        return pushDate.getMinPushHour() < pushDate.getMaxPushHour();
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return random.nextInt((max - min) + 1) + min;
    }

    public static Optional<LocalDateTime> toLocalDateTime(Date date) {

        return Optional.ofNullable(Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.of(UTC))
                .toLocalDateTime());
    }

    public static Date getNowAtTimeZoneUTC() {
        return Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of(UTC))
                .toInstant());
    }

    public static Date getNowZoneUTC() {
        LocalDateTime datetime = LocalDateTime.now().atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneId.of(UTC)).toLocalDateTime();
        return toSqlDate(datetime);
    }
    public static Date toSqlDate(LocalDateTime date) {

        return java.sql.Timestamp.valueOf(date);
    }

    public static Optional<Date> getNextPushDate(PushDate pushDate) {

        if (Objects.nonNull(pushDate)) {
            if (!isValidHour(pushDate)) {
                log.warn("Invalid hours (minHour >= maxHour) {} > {}", pushDate.getMinPushHour(), pushDate.getMaxPushHour());
                return Optional.empty();
            }

            if(StringUtils.isBlank(pushDate.getTimezone())) {
                log.warn("The timezone should not be null or blank");
                return Optional.empty();
            }

            if(Objects.isNull(pushDate.getLastPushDate())) {
                log.warn("The last push date is null. Using now");
                pushDate.setLastPushDate(getNowAtTimeZoneUTC());
            }

            try {
                // Convert to timezone
                Optional<LocalDateTime> dateTime = toDateAtTimezone(pushDate);
                if(!dateTime.isPresent()) {
                    log.warn("Failed to convert the push date at timezone");
                    return Optional.empty();
                }

                LocalDateTime dateAtTimezone = null;

                do {
                    int pushHour = getRandomNumberInRange (pushDate.getMinPushHour(),  pushDate.getMaxPushHour());
                    // add distribution on minutes ==> allowing to execute the batch every x minutes instead of every hour
                    int pushMinute = getRandomNumberInRange(0,59);
                    dateAtTimezone = dateTime.get().toLocalDate().plusDays(1).atStartOfDay().withHour(pushHour).withMinute(pushMinute);

                } while(!isBetween(dateAtTimezone, dateAtTimezone.withHour(pushDate.getMinPushHour()), dateAtTimezone.withHour(pushDate.getMaxPushHour())));

                return toDateTimezoneUTC(dateAtTimezone, pushDate.getTimezone());
            } catch (DateTimeException e) {
                log.error("Failed to calculate the next push date due to {}", e.getMessage());
            }
        }
        return Optional.empty();
    }


    public static boolean isDateBetween(Date dateToCompare, Date dateDebut, Date dateFin){

        return Optional.ofNullable(dateToCompare)
                .filter(date -> Objects.nonNull(dateDebut))
                .filter(date -> Objects.nonNull(dateFin))
                .map(date -> {

                    LocalDateTime dateInitiale = toLocalDateTime(dateToCompare).get();
                    LocalDateTime dateDeDebut = toLocalDateTime(dateDebut).get();
                    LocalDateTime dateDeFin = toLocalDateTime(dateFin).get();
                    return isBetween(dateInitiale, dateDeDebut, dateDeFin);
                }).orElse(false);
    }

    public static boolean isBetween(LocalDateTime dateToCompare, LocalDateTime dateDebut, LocalDateTime dateFin){

        return Optional.ofNullable(dateToCompare)
                .filter(date -> Objects.nonNull(dateDebut))
                .filter(date -> Objects.nonNull(dateFin))
                .filter(date -> HOURS.between(dateDebut, dateFin) > 0)
                .map(date -> {

                    long secondssAfterDebut = HOURS.between(dateDebut, date);
                    long secondsBeforeFin = HOURS.between(date, dateFin);
                    return secondssAfterDebut >= 0 && secondsBeforeFin >= 0;

                }).orElse(false);
    }
}
