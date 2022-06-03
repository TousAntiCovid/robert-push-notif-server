package fr.gouv.stopc.robert.pushnotif.scheduler.repository;

import javax.persistence.AttributeConverter;

import java.sql.Timestamp;
import java.time.Instant;

public class TimeStampInstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attribute) {
        return attribute == null ? null : Timestamp.from(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp dbData) {
        return dbData == null ? null : dbData.toInstant();
    }
}
