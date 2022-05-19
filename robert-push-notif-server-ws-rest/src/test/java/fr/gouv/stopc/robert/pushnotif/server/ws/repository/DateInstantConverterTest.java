package fr.gouv.stopc.robert.pushnotif.server.ws.repository;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.*;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DateInstantConverterTest {

    private final TimeStampInstantAttributeConverter dateInstantConverter = new TimeStampInstantAttributeConverter();

    @Test
    public void instant_to_timestamp() {
        int hoursBetweenUTCAndLocalTimeZone = ZonedDateTime.now().getOffset().getTotalSeconds() / 3600;
        assertThat(dateInstantConverter.convertToDatabaseColumn(Instant.ofEpochSecond(1666618560L)))
                .hasTime(1666618560000L)
                .hasDayOfMonth(24)
                .hasMonth(10)
                .hasYear(2022)
                .hasHourOfDay(13 + hoursBetweenUTCAndLocalTimeZone)
                .hasMinute(36);
    }

    @Test
    public void timestamp_to_instant() {
        final var timestamp = Timestamp.from(LocalDateTime.of(2022, 10, 24, 13, 36).toInstant(UTC));
        assertThat(dateInstantConverter.convertToEntityAttribute(timestamp).getEpochSecond()).isEqualTo(1666618560L);
    }
}