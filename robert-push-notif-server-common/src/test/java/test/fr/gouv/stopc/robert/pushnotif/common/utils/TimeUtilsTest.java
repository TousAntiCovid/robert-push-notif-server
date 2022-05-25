package test.fr.gouv.stopc.robert.pushnotif.common.utils;

import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TimeUtilsTest {

    @Test
    public void testConstructorShouldThrowAssertionError() {

        // Then
        Assertions.assertThrows(AssertionError.class, () -> {
            Constructor<TimeUtils> constructor;
            try {
                // Given
                constructor = TimeUtils.class.getDeclaredConstructor(null);
                assertNotNull(constructor);
                constructor.setAccessible(true);
                // When
                constructor.newInstance(null);
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                Assertions.fail("Should not throw these exceptions");
            }
        });
    }

    private LocalDateTime getMinNextPushDate(PushDate pushDate) {

        ZonedDateTime now = toDateAtTimezone(pushDate.getLastPushDate(), pushDate)
                .toLocalDate().atStartOfDay(ZoneId.of(pushDate.getTimezone()))
                .plusDays(1);
        return now.withHour(pushDate.getMinPushHour()).toLocalDateTime();
    }

    private LocalDateTime getMaxNextPushDate(PushDate pushDate) {

        ZonedDateTime now = toDateAtTimezone(pushDate.getLastPushDate(), pushDate)
                .toLocalDate().atStartOfDay(ZoneId.of(pushDate.getTimezone())).plusDays(1);
        return now.withHour(pushDate.getMaxPushHour()).toLocalDateTime();
    }

    private LocalDateTime toDateAtTimezone(Date nextPushDate, PushDate pushdate) {

        return Instant.ofEpochMilli(nextPushDate.getTime())
                .atZone(ZoneId.of(TimeUtils.UTC))
                .withZoneSameInstant(ZoneId.of(pushdate.getTimezone()))
                .toLocalDateTime();
    }

    private boolean isBetween(Date nextPushDate, PushDate pushDate) {

        LocalDateTime minNextPushDate = this.getMinNextPushDate(pushDate);
        LocalDateTime maxNextPushDate = this.getMaxNextPushDate(pushDate);

        // When
        LocalDateTime nextPushDateime = this.toDateAtTimezone(nextPushDate, pushDate);

        // Then
        return TimeUtils.isBetween(nextPushDateime, minNextPushDate, maxNextPushDate);
    }

    @Test
    public void testGetNextPushDateWhenPushDateIsNull() {

        // Given
        PushDate pushDate = null;

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertFalse(nextPushDate.isPresent());
    }

    @Test
    public void testGetNextPushDateWhenPushDateMinHourIsGreaterThanMaxHour() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(17)
                .maxPushHour(10)
                .lastPushDate(new Date())
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertFalse(nextPushDate.isPresent());
    }

    @Test
    public void testGetNextPushDateWhenTimezoneIsNull() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(6)
                .maxPushHour(10)
                .lastPushDate(new Date())
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertFalse(nextPushDate.isPresent());
    }

    @Test
    public void testGetNextPushDateWhenTimezoneIsBlank() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(6)
                .maxPushHour(10)
                .timezone("")
                .lastPushDate(new Date())
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertFalse(nextPushDate.isPresent());
    }

    @Test
    public void testGetNextPushDateWhenTimezoneIsInvalid() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(6)
                .maxPushHour(10)
                .timezone("Fake timezone")
                .lastPushDate(new Date())
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertFalse(nextPushDate.isPresent());
    }

    @RepeatedTest(1000)
    public void testGetNextPushDateSucceeds() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(6)
                .maxPushHour(10)
                .timezone("Europe/Paris")
                .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertTrue(nextPushDate.isPresent());
        assertTrue(this.isBetween(nextPushDate.get(), pushDate));
    }

    @RepeatedTest(1000)
    public void testGetNextPushDateSucceedsWhenLastPushDateIsNull() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(6)
                .maxPushHour(10)
                .timezone("Europe/Paris")
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertTrue(nextPushDate.isPresent());
        assertTrue(this.isBetween(nextPushDate.get(), pushDate));
    }

    @RepeatedTest(1000)
    public void testGetNextPushDateSucceedsWithAnotherTimezone() {

        // Given
        PushDate pushDate = PushDate.builder()
                .minPushHour(8)
                .maxPushHour(19)
                .timezone("America/Cayenne")
                .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                .build();

        // When
        Optional<Date> nextPushDate = TimeUtils.getNextPushDate(pushDate);

        // Then
        assertTrue(nextPushDate.isPresent());
        assertTrue(this.isBetween(nextPushDate.get(), pushDate));
    }

    @Test
    public void testDateIsBetweenWhenAllIsNull() {

        // When
        assertFalse(TimeUtils.isBetween(null, null, null));
    }

    @Test
    public void testDateIsBetweenWhenDateDebutIsNull() {

        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateFin = LocalDateTime.now().plusHours(5);

        // When && Then
        assertFalse(TimeUtils.isBetween(now, null, dateFin));
    }

    @Test
    public void testDateIsBetweenWhenDateFinIsNull() {

        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateDebut = LocalDateTime.now().minusHours(5);

        // When && Then
        assertFalse(TimeUtils.isBetween(now, dateDebut, null));
    }

    @Test
    public void testDateIsBetweenWhenEqualsDateDebutAndDateFin() {

        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateDebut = LocalDateTime.now().minusHours(5);
        LocalDateTime dateFin = dateDebut;

        // When && Then
        assertFalse(TimeUtils.isBetween(now, dateDebut, dateFin));
    }

    @Test
    public void testDateIsBetweenWhenDateToCompareIsTheSameThanDateDebut() {

        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateFin = LocalDateTime.now().plusHours(5);

        // When && Then
        assertTrue(TimeUtils.isBetween(now, now, dateFin));
    }

    @Test
    public void testDateIsBetweenWhenDateToCompareIsTheSameThanDateFin() {

        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateDebut = LocalDateTime.now().minusHours(5);

        // When && Then
        assertTrue(TimeUtils.isBetween(now, dateDebut, now));
    }

    @Test
    public void testDateIsBetweenWhenDateToCompareIsBeforeDateDebut() {

        // Given
        LocalDateTime dateCompare = LocalDateTime.now().minusHours(1);
        LocalDateTime dateDebut = LocalDateTime.now();
        LocalDateTime dateFin = LocalDateTime.now().plusHours(5);

        // When && Then
        assertFalse(TimeUtils.isBetween(dateCompare, dateDebut, dateFin));
    }

    @Test
    public void testDateIsBetweenWhenDateToCompareIsAfterDateFin() {

        // Given
        LocalDateTime dateCompare = LocalDateTime.now().plusHours(6);
        LocalDateTime dateDebut = LocalDateTime.now();
        LocalDateTime dateFin = LocalDateTime.now().plusHours(4);

        // When && Then
        assertFalse(TimeUtils.isBetween(dateCompare, dateDebut, dateFin));
    }

    @Test
    public void testDateIsBetweenWhenDateToCompareIsBetween() {

        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateDebut = LocalDateTime.now().minusHours(5);
        LocalDateTime dateFin = LocalDateTime.now().plusHours(5);

        // When && Then
        assertTrue(TimeUtils.isBetween(now, dateDebut, dateFin));
    }

    @Test
    public void testDateIsDateBetweenWhenAllIsNull() {

        // When
        assertFalse(TimeUtils.isDateBetween(null, null, null));
    }

    @Test
    public void testDateIsDateBetweenWhenDateDebutIsNull() {

        // Given
        Date now = TimeUtils.toSqlDate(LocalDateTime.now());
        Date dateFin = TimeUtils.toSqlDate(LocalDateTime.now().plusDays(5));

        // When && Then
        assertFalse(TimeUtils.isDateBetween(now, null, dateFin));
    }

    @Test
    public void testDateIsDateBetweenWhenDateFinIsNull() {

        // Given
        Date now = TimeUtils.toSqlDate(LocalDateTime.now());
        Date dateDebut = TimeUtils.toSqlDate(LocalDateTime.now().minusDays(5));

        // When && Then
        assertFalse(TimeUtils.isDateBetween(now, dateDebut, null));
    }

    @Test
    public void testDateIsDateBetweenWhenEqualsDateDebutAndDateFin() {

        // Given
        Date now = TimeUtils.toSqlDate(LocalDateTime.now());
        Date dateDebut = TimeUtils.toSqlDate(LocalDateTime.now().minusDays(5));
        Date dateFin = dateDebut;

        // When && Then
        assertFalse(TimeUtils.isDateBetween(now, dateDebut, dateFin));
    }

    @Test
    public void testDateIsDateBetweenWhenDateToCompareIsTheSameThanDateDebut() {

        // Given
        Date now = TimeUtils.toSqlDate(LocalDateTime.now());
        Date dateFin = TimeUtils.toSqlDate(LocalDateTime.now().plusDays(5));

        // When && Then
        assertTrue(TimeUtils.isDateBetween(now, now, dateFin));
    }

    @Test
    public void testDateIsDateBetweenWhenDateToCompareIsTheSameThanDateFin() {

        // Given
        Date now = TimeUtils.toSqlDate(LocalDateTime.now());
        Date dateDebut = TimeUtils.toSqlDate(LocalDateTime.now().minusDays(5));

        // When && Then
        assertTrue(TimeUtils.isDateBetween(now, dateDebut, now));
    }

    @Test
    public void testDateIsDateBetweenWhenDateToCompareIsBetween() {

        // Given
        Date now = TimeUtils.toSqlDate(LocalDateTime.now());
        Date dateDebut = TimeUtils.toSqlDate(LocalDateTime.now().minusDays(5));
        Date dateFin = TimeUtils.toSqlDate(LocalDateTime.now().plusDays(5));

        // When && Then
        assertTrue(TimeUtils.isDateBetween(now, dateDebut, dateFin));
    }

}
