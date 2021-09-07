package fr.gouv.stopc.robert.pushnotif.scheduler.it;

import fr.gouv.stopc.robert.pushnotif.scheduler.dao.mapper.PushInfoRowMapper;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PushInfoToolsDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final PushInfoRowMapper pushInfoRowMapper = new PushInfoRowMapper();

    private SimpleJdbcInsert simpleJdbcInsert;

    @PostConstruct
    public void init() {
        simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("push");
    }

    public int insert(final PushInfo pushInfo) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", pushInfo.getId());
        parameters.addValue("creation_date", pushInfo.getCreationDate());
        parameters.addValue("locale", pushInfo.getLocale());
        parameters.addValue("timezone", pushInfo.getTimezone());
        parameters.addValue("token", pushInfo.getToken());
        parameters.addValue("active", pushInfo.isActive());
        parameters.addValue("deleted", pushInfo.isDeleted());
        parameters.addValue("successful_push_sent", pushInfo.getSuccessfulPushSent());
        parameters.addValue("last_successful_push", pushInfo.getLastSuccessfulPush());
        parameters.addValue("failed_push_sent", pushInfo.getFailedPushSent());
        parameters.addValue("last_failure_push", pushInfo.getLastFailurePush());
        parameters.addValue("last_error_code", pushInfo.getLastErrorCode());
        parameters.addValue("next_planned_push", pushInfo.getNextPlannedPush());
        return simpleJdbcInsert.execute(parameters);
    }

    public PushInfo findByToken(String token) {
        final var parameters = Map.of("token", token);
        return jdbcTemplate.queryForObject("select * from push where token = :token", parameters, pushInfoRowMapper);
    }

    public List<PushInfo> findAll() {
        return jdbcTemplate.query("select * from push ", pushInfoRowMapper);
    }

}
