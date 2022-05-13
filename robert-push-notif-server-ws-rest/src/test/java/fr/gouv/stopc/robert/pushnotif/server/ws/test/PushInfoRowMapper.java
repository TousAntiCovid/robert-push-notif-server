package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class PushInfoRowMapper implements RowMapper<PushInfo> {

    @Override
    public PushInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        return PushInfo.builder()
                .id(rs.getLong("id"))
                .token(rs.getString("token"))
                .timezone(rs.getString("timezone"))
                .locale(rs.getString("locale"))
                .active(rs.getBoolean("active"))
                .deleted(rs.getBoolean("deleted"))
                .nextPlannedPush(Date.from(rs.getTimestamp("next_planned_push").toInstant()))
                .build();
    }
}
