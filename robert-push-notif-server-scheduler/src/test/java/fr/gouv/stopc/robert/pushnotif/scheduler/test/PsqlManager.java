package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import fr.gouv.stopc.robert.pushnotif.scheduler.data.PushInfoRowMapper;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo.PushInfoBuilder;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static fr.gouv.stopc.robert.pushnotif.scheduler.data.InstantTimestampConverter.convertInstantToTimestamp;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;

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

    static PushInfoBuilder pushinfoBuilder = PushInfo.builder()
            .id(10000000L)
            .active(true)
            .deleted(false)
            .token("00000000")
            .locale("fr-FR")
            .creationDate(now())
            .successfulPushSent(0)
            .failedPushSent(0)
            .timezone("Europe/Paris");

    public static void givenPushInfoWith(final Function<PushInfoBuilder, PushInfoBuilder> testSpecificBuilder) {
        insert(
                testSpecificBuilder.apply(
                        pushinfoBuilder
                                /*
                                 * Set next planned push date outside of static builder to have varying
                                 * getRandomNumberInRange results but let test specific builder override it if
                                 * needed
                                 */
                                .nextPlannedPush(
                                        LocalDateTime.from(
                                                LocalDate.now().atStartOfDay().plusHours(new Random().nextInt(24))
                                                        .plusMinutes(new Random().nextInt(60)).minusDays(1)
                                        )
                                                .toInstant(UTC)
                                )
                ).build()
        );
    }

    public static PushInfo findByToken(final String token) {
        final var parameters = Map.of("token", token);
        return jdbcTemplate.queryForObject("select * from push where token = :token", parameters, pushInfoRowMapper);
    }

    public static List<PushInfo> findAll() {
        return jdbcTemplate.query("select * from push ", pushInfoRowMapper);
    }

}
