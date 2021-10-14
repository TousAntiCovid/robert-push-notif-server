package fr.gouv.stopc.robert.pushnotif.scheduler.dao;

import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PushInfoDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public int updateNextPlannedPushDate(final PushInfo pushInfo) {
        return jdbcTemplate
                .update(
                        "update push set next_planned_push = :nextPlannedPushDate where id = :id",
                        Map.of(
                                "id", pushInfo.getId(),
                                "nextPlannedPushDate", pushInfo.getNextPlannedPush()
                        )
                );

    }

    @Transactional
    public int updateSuccessFulPushedNotif(final PushInfo pushInfo) {
        return jdbcTemplate.update(
                "update push set last_successful_push = :lastSuccessfulPush, " +
                        "successful_push_sent = :successfulPushSent " +
                        "where id = :id",
                Map.of(
                        "id", pushInfo.getId(),
                        "lastSuccessfulPush", pushInfo.getLastSuccessfulPush(),
                        "successfulPushSent", pushInfo.getSuccessfulPushSent()
                )
        );

    }

    @Transactional
    public int updateFailurePushedNotif(final PushInfo pushInfo) {

        return jdbcTemplate.update(
                "update push set active = :active, " +
                        "last_failure_push = :lastFailurePush, " +
                        "failed_push_sent = :failedPushSent, " +
                        "last_error_code = :lastErrorCode " +
                        "where id = :id",
                Map.of(
                        "id", pushInfo.getId(),
                        "active", pushInfo.isActive(),
                        "lastFailurePush", pushInfo.getLastFailurePush(),
                        "failedPushSent", pushInfo.getFailedPushSent(),
                        "lastErrorCode", pushInfo.getLastErrorCode()
                )
        );

    }

}
