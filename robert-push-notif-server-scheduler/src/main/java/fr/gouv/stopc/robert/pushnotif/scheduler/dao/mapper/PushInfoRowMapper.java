package fr.gouv.stopc.robert.pushnotif.scheduler.dao.mapper;

import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class PushInfoRowMapper implements RowMapper<PushInfo> {

    @Override
    public PushInfo mapRow(ResultSet resultSet, int i) throws SQLException {
        return PushInfo.builder()
                .id(resultSet.getLong("id"))
                .creationDate(convertTimestampToInstant(resultSet.getTimestamp("creation_date")))
                .locale(resultSet.getString("locale"))
                .timezone(resultSet.getString("timezone"))
                .token(resultSet.getString("token"))
                .active(resultSet.getBoolean("active"))
                .deleted(resultSet.getBoolean("deleted"))
                .successfulPushSent(resultSet.getInt("successful_push_sent"))
                .lastSuccessfulPush(convertTimestampToInstant(resultSet.getTimestamp("last_successful_push")))
                .failedPushSent(resultSet.getInt("failed_push_sent"))
                .lastFailurePush(convertTimestampToInstant(resultSet.getTimestamp("last_failure_push")))
                .lastErrorCode(resultSet.getString("last_error_code"))
                .nextPlannedPush(convertTimestampToInstant(resultSet.getTimestamp("next_planned_push")))
                .build();
    }

    private LocalDateTime convertTimestampToInstant(final Timestamp timestamp) {
        return timestamp != null ? LocalDateTime.ofInstant(timestamp.toInstant(), ZoneOffset.UTC) : null;
    }
}
