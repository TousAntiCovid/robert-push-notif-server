package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import lombok.Builder;
import lombok.Value;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

public class PsqlManager implements TestExecutionListener {

    private static NamedParameterJdbcTemplate jdbcTemplate;

    private static final PushInfoRowMapper pushInfoRowMapper = new PushInfoRowMapper();

    private static final JdbcDatabaseContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:13.7")
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

    public static void givenPushInfoForToken(String token) {
        givenPushInfoForTokenAndNextPlannedPush(
                token,
                LocalDateTime.from(
                        LocalDate.now().atStartOfDay().plusHours(new Random().nextInt(24))
                                .plusMinutes(new Random().nextInt(60)).minusDays(1)
                )
                        .toInstant(UTC)
        );
    }

    public static void givenPushInfoForTokenAndNextPlannedPush(String token, Instant nextPlannedPush) {
        new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("push")
                .usingGeneratedKeyColumns("id")
                .execute(
                        Map.of(
                                "creation_date", Timestamp.from(Instant.now()),
                                "locale", "fr-FR",
                                "timezone", "Europe/Paris",
                                "token", token,
                                "active", true,
                                "deleted", false,
                                "successful_push_sent", 0,
                                "failed_push_sent", 0,
                                "next_planned_push", Timestamp.from(nextPlannedPush)
                        )
                );
    }

    public static ListAssert<PushInfo> assertThatAllPushInfo() {
        return assertThat(jdbcTemplate.query("select * from push", Map.of(), PushInfoRowMapper.INSTANCE))
                .as("all push data stored");
    }

    public static ObjectAssert<PushInfo> assertThatPushInfo(final String token) {
        final var push = jdbcTemplate.queryForObject(
                "select * from push where token = :token", Map.of("token", token),
                PushInfoRowMapper.INSTANCE
        );
        return assertThat(push)
                .describedAs("push data for token %s", token);
    }

    @Value
    @Builder
    public static class PushInfo {

        String token;

        boolean active;

        boolean deleted;

        int successfulPushSent;

        Instant lastSuccessfulPush;

        int failedPushSent;

        Instant lastFailurePush;

        String lastErrorCode;

        Instant nextPlannedPush;
    }

    private static class PushInfoRowMapper implements RowMapper<PushInfo> {

        private static final PushInfoRowMapper INSTANCE = new PushInfoRowMapper();

        @Override
        public PushInfo mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return PushInfo.builder()
                    .token(rs.getString("token"))
                    .active(rs.getBoolean("active"))
                    .deleted(rs.getBoolean("deleted"))
                    .successfulPushSent(rs.getInt("successful_push_sent"))
                    .lastSuccessfulPush(toInstant(rs.getTimestamp("last_successful_push")))
                    .failedPushSent(rs.getInt("failed_push_sent"))
                    .lastFailurePush(toInstant(rs.getTimestamp("last_failure_push")))
                    .lastErrorCode(rs.getString("last_error_code"))
                    .nextPlannedPush(toInstant(rs.getTimestamp("next_planned_push")))
                    .build();
        }

        private static Instant toInstant(final Timestamp timestamp) {
            return timestamp != null ? timestamp.toInstant() : null;
        }
    }
}
