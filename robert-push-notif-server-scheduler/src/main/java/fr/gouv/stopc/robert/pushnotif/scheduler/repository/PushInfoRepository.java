package fr.gouv.stopc.robert.pushnotif.scheduler.repository;

import fr.gouv.stopc.robert.pushnotif.scheduler.repository.model.PushInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PushInfoRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public void forEachNotificationToBeSent(final Consumer<PushInfo> pushInfoHandler) {
        jdbcTemplate.query(
                "select * from push where active = true and deleted = false and next_planned_push <= now()",
                rs -> {
                    final var pushInfo = PushInfo.builder()
                            .id(rs.getLong("id"))
                            .timezone(rs.getString("timezone"))
                            .token(rs.getString("token"))
                            .build();
                    pushInfoHandler.accept(pushInfo);
                }
        );
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void updateNextPlannedPushDate(final long id, final Instant nextPlannedPush) {
        jdbcTemplate.update(
                "update push set next_planned_push = :nextPlannedPushDate where id = :id", Map.of(
                        "id", id,
                        "nextPlannedPushDate", Timestamp.from(nextPlannedPush)
                )
        );
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void updateSuccessfulPushSent(final long id) {
        jdbcTemplate.update(
                "update push set last_successful_push = :lastSuccessfulPush, " +
                        "successful_push_sent = successful_push_sent + 1 " +
                        "where id = :id",
                Map.of(
                        "id", id,
                        "lastSuccessfulPush", Timestamp.from(Instant.now())
                )
        );

    }

    @Transactional(propagation = REQUIRES_NEW)
    public void updateFailure(final long id, final String failureDescription) {
        jdbcTemplate.update(
                "update push set " +
                        "last_failure_push = :lastFailurePush, " +
                        "failed_push_sent = failed_push_sent + 1, " +
                        "last_error_code = :lastErrorCode::char(255) " +
                        "where id = :id",
                Map.of(
                        "id", id,
                        "lastFailurePush", Timestamp.from(Instant.now()),
                        "lastErrorCode", failureDescription
                )
        );
    }

    @Transactional(propagation = REQUIRES_NEW)
    public void disable(final Long id) {
        jdbcTemplate.update("update push set active = false where id = :id", Map.of("id", id));
    }
}
