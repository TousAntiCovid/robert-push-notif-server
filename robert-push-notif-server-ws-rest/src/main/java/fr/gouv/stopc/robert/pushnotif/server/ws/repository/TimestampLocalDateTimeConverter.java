package fr.gouv.stopc.robert.pushnotif.server.ws.repository;

import javax.persistence.AttributeConverter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TimestampLocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute == null ? null : Timestamp.valueOf(attribute);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp dbData) {
        return dbData == null ? null : dbData.toLocalDateTime();
    }
}
