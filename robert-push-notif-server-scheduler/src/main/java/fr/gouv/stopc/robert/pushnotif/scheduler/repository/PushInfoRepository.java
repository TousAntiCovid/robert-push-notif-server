package fr.gouv.stopc.robert.pushnotif.scheduler.repository;

import fr.gouv.stopc.robert.pushnotif.scheduler.repository.model.PushInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

import static fr.gouv.stopc.robert.pushnotif.scheduler.repository.InstantTimestampConverter.convertInstantToTimestamp;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Repository
@RequiredArgsConstructor
public class PushInfoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public void forEachNotificationToBeSent(final Consumer<PushInfo> pushInfoHandler) {
        jdbcTemplate.query(
                "select * from push where active = true and deleted = false and next_planned_push <= now()",
                rs -> {
                    final var pushInfo = PushInfoRowMapper.INSTANCE.mapRow(rs, rs.getRow());
                    pushInfoHandler.accept(pushInfo);
                }
        );
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void updateNextPlannedPushDate(final PushInfo pushInfo) {
        final var params = new MapSqlParameterSource();
        params.addValue("id", pushInfo.getId());
        params.addValue(
                "nextPlannedPushDate", convertInstantToTimestamp(pushInfo.getNextPlannedPush())
        );
        jdbcTemplate.update("update push set next_planned_push = :nextPlannedPushDate where id = :id", params);
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void updateSuccessFulPushedNotif(final PushInfo pushInfo) {
        final var params = new MapSqlParameterSource();
        params.addValue("id", pushInfo.getId());
        params.addValue(
                "lastSuccessfulPush", convertInstantToTimestamp(pushInfo.getLastSuccessfulPush())
        );
        params.addValue("successfulPushSent", pushInfo.getSuccessfulPushSent());

        jdbcTemplate.update(
                "update push set last_successful_push = :lastSuccessfulPush, " +
                        "successful_push_sent = :successfulPushSent " +
                        "where id = :id",
                params
        );

    }

    @Transactional(propagation = REQUIRES_NEW)
    public void updateFailurePushedNotif(final PushInfo pushInfo) {
        final var params = new MapSqlParameterSource();
        params.addValue("id", pushInfo.getId());
        params.addValue("active", pushInfo.isActive());
        params.addValue("lastFailurePush", convertInstantToTimestamp(pushInfo.getLastFailurePush()));
        params.addValue("failedPushSent", pushInfo.getFailedPushSent());
        params.addValue("lastErrorCode", pushInfo.getLastErrorCode());

        jdbcTemplate.update(
                "update push set active = :active, " +
                        "last_failure_push = :lastFailurePush, " +
                        "failed_push_sent = :failedPushSent, " +
                        "last_error_code = :lastErrorCode " +
                        "where id = :id",
                params
        );
    }

}
