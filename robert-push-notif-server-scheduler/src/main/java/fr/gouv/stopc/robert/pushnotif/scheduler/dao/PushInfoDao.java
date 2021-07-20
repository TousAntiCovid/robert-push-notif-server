package fr.gouv.stopc.robert.pushnotif.scheduler.dao;

import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PushInfoDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public int updateNextPlannedPushDate(final PushInfo pushInfo) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", pushInfo.getId());
        parameters.addValue("nextPlannedPushDate", pushInfo.getNextPlannedPush());
        return jdbcTemplate
                .update("update push set next_planned_push = :nextPlannedPushDate where id = :id", parameters);

    }

    @Transactional
    public int updateSuccessFulPushedNotif(final PushInfo pushInfo) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", pushInfo.getId());
        parameters.addValue("lastSuccessfulPush", pushInfo.getLastSuccessfulPush());
        parameters.addValue("successfulPushSent", pushInfo.getSuccessfulPushSent());
        return jdbcTemplate.update(
                "update push set last_successful_push = :lastSuccessfulPush, " +
                        "successful_push_sent = :successfulPushSent " +
                        "where id = :id",
                parameters
        );

    }

    @Transactional
    public int updateFailurePushedNotif(final PushInfo pushInfo) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", pushInfo.getId());
        parameters.addValue("active", pushInfo.isActive());
        parameters.addValue("lastFailurePush", pushInfo.getLastFailurePush());
        parameters.addValue("failedPushSent", pushInfo.getFailedPushSent());
        parameters.addValue("lastErrorCode", pushInfo.getLastErrorCode());

        return jdbcTemplate.update(
                "update push set active = :active, " +
                        "last_failure_push = :lastFailurePush, " +
                        "failed_push_sent = :failedPushSent, " +
                        "last_error_code = :lastErrorCode " +
                        "where id = :id",
                parameters
        );

    }

}
