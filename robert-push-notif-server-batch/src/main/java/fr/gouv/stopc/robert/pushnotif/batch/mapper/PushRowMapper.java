package fr.gouv.stopc.robert.pushnotif.batch.mapper;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

public class PushRowMapper implements RowMapper<PushInfo> {

    @Override
    public PushInfo mapRow(ResultSet rs, int rowNum) throws SQLException {

        return PushInfo.builder()
                .id(rs.getLong("id"))
                .active(rs.getBoolean("active"))
                .deleted(rs.getBoolean("deleted"))
                .nextPlannedPush(this.getDateFromTimestamp(rs, "next_planned_push"))
                .token(rs.getString("token"))
                .timezone(rs.getString("timezone"))
                .locale(rs.getString("locale"))
                .successfulPushSent(rs.getInt("successful_push_sent"))
                .lastFailurePush(this.getDateFromTimestamp(rs, "last_failure_push"))
                .lastSuccessfulPush(this.getDateFromTimestamp(rs, "last_successful_push"))
                .lastErrorCode(rs.getString("last_error_code"))
                .failedPushSent(rs.getInt("failed_push_sent"))
                .build();
    }

    private Date getDateFromTimestamp(ResultSet rs, String columnName) throws SQLException {
        Timestamp nextPushDateTimestamp = rs.getTimestamp(columnName);
        return Optional.ofNullable(nextPushDateTimestamp).map(timestamp -> new java.util.Date(timestamp.getTime()))
                .orElse(null);
    }
}
