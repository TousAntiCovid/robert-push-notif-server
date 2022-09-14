package fr.gouv.stopc.robert.pushnotif.scheduler.data;

import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static fr.gouv.stopc.robert.pushnotif.scheduler.data.InstantTimestampConverter.convertTimestampToInstant;

public class PushInfoRowMapper implements RowMapper<PushInfo> {

    public static final RowMapper<PushInfo> INSTANCE = new PushInfoRowMapper();

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
}
