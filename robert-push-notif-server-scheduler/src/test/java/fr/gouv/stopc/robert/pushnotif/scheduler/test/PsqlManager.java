package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import fr.gouv.stopc.robert.pushnotif.scheduler.data.PushInfoRowMapper;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static fr.gouv.stopc.robert.pushnotif.scheduler.data.InstantTimestampConverter.convertInstantToTimestamp;

public class PsqlManager implements TestExecutionListener {

    private static NamedParameterJdbcTemplate jdbcTemplate;

    private static final PushInfoRowMapper pushInfoRowMapper = new PushInfoRowMapper();

    private static int insert(final PushInfo pushInfo) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("id", pushInfo.getId());
        parameters.addValue("creation_date", convertInstantToTimestamp(pushInfo.getCreationDate()));
        parameters.addValue("locale", pushInfo.getLocale());
        parameters.addValue("timezone", pushInfo.getTimezone());
        parameters.addValue("token", pushInfo.getToken());
        parameters.addValue("active", pushInfo.isActive());
        parameters.addValue("deleted", pushInfo.isDeleted());
        parameters.addValue("successful_push_sent", pushInfo.getSuccessfulPushSent());
        parameters.addValue("last_successful_push", convertInstantToTimestamp(pushInfo.getLastSuccessfulPush()));
        parameters.addValue("failed_push_sent", pushInfo.getFailedPushSent());
        parameters.addValue("last_failure_push", convertInstantToTimestamp(pushInfo.getLastFailurePush()));
        parameters.addValue("last_error_code", pushInfo.getLastErrorCode());
        parameters.addValue("next_planned_push", convertInstantToTimestamp(pushInfo.getNextPlannedPush()));
        return new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("push")
                .execute(parameters);
    }

    private static final JdbcDatabaseContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:9.6")
    );

    static {
        POSTGRES.withCommand("-c", "log_statement=all");
        POSTGRES.start();
        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        jdbcTemplate.getJdbcTemplate().execute("drop schema public cascade;");
        jdbcTemplate.getJdbcTemplate().execute("create schema public;");
        testContext.getApplicationContext().getBean(Flyway.class).migrate();
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        jdbcTemplate = testContext.getApplicationContext().getBean(NamedParameterJdbcTemplate.class);
    }

    public static void givenOnePushInfoSuchAs(final PushInfo pushInfo) {
        insert(pushInfo);
    }

    public static PushInfo findByToken(String token) {
        final var parameters = Map.of("token", token);
        return jdbcTemplate.queryForObject("select * from push where token = :token", parameters, pushInfoRowMapper);
    }

    public static List<PushInfo> findAll() {
        return jdbcTemplate.query("select * from push ", pushInfoRowMapper);
    }
}