package fr.gouv.stopc.robert.pushnotif.server.ws.repository;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class DateInstantConverterTest {

    private final TimestampLocalDateTimeConverter dateInstantConverter = new TimestampLocalDateTimeConverter();

    @Test
    public void localDateTime_to_date() {
        final var localDateTime = LocalDateTime.of(2022, 10, 24, 13, 36);
        assertThat(dateInstantConverter.convertToDatabaseColumn(localDateTime))
                .hasDayOfMonth(24)
                .hasMonth(10)
                .hasYear(2022)
                .hasHourOfDay(13)
                .hasMinute(36);
    }

    @Test
    public void date_to_localDateTime() {
        final var timestamp = Timestamp.valueOf(LocalDateTime.of(2022, 10, 24, 13, 36));
        assertThat(dateInstantConverter.convertToEntityAttribute(timestamp)).isEqualTo("2022-10-24T13:36:00");
    }
}
