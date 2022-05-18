package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class PsqlManager implements TestExecutionListener {

    static JdbcTemplate jdbcTemplate;

    private static final JdbcDatabaseContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:9.6")
    );

    public static LocalDateTime defaultNextPlannedPushDate = LocalDateTime.now();

    static {
        POSTGRES.start();
        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        jdbcTemplate.execute("drop schema public cascade ;");
        jdbcTemplate.execute("drop table if exists push cascade ;");
        jdbcTemplate.execute("create schema public ;");
        jdbcTemplate.execute(
                "create table push (id  bigserial not null, active boolean, creation_date timestamp, deleted boolean, failed_push_sent int4, last_error_code varchar(255), last_failure_push timestamp, last_successful_push timestamp, locale varchar(255) not null, next_planned_push timestamp, successful_push_sent int4, timezone varchar(255) not null, token varchar(255) not null, primary key (id));"
        );
        jdbcTemplate.execute("create index IDX_TOKEN on push (token);");
        jdbcTemplate.execute("alter table push add constraint UK_aw1ibs9m7eqsdmrin9p3b37i1 unique (token);");
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        jdbcTemplate = testContext.getApplicationContext().getBean(JdbcTemplate.class);
    }

    public static void givenOneFrPushInfoWith(final String token) {
        PushInfo pushInfo = PushInfo.builder()
                .token(token)
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .nextPlannedPush(defaultNextPlannedPushDate)
                .build();
        givenOnePushInfoSuchAs(pushInfo);
    }

    public static void givenOnePushInfoSuchAs(final PushInfo pushInfo) {
        final String query = "insert into push (token, timezone, locale, next_planned_push, last_successful_push, last_failure_push, last_error_code, successful_push_sent, failed_push_sent, creation_date, active, deleted) values (?,?,?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(
                query,
                pushInfo.getToken(),
                pushInfo.getTimezone(),
                pushInfo.getLocale(),
                pushInfo.getNextPlannedPush(),
                pushInfo.getLastSuccessfulPush(),
                pushInfo.getLastFailurePush(),
                pushInfo.getLastErrorCode(),
                pushInfo.getSuccessfulPushSent(),
                pushInfo.getFailedPushSent(),
                pushInfo.getCreationDate(),
                pushInfo.isActive(),
                pushInfo.isDeleted()
        );
    }

    public static List<PushInfo> getPushInfos() {
        return jdbcTemplate.query("select * from push", new PushInfoRowMapper());
    }

    private static class PushInfoRowMapper implements RowMapper<PushInfo> {

        @Override
        public PushInfo mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return PushInfo.builder()
                    .id(rs.getLong("id"))
                    .token(rs.getString("token"))
                    .timezone(rs.getString("timezone"))
                    .locale(rs.getString("locale"))
                    .active(rs.getBoolean("active"))
                    .deleted(rs.getBoolean("deleted"))
                    .nextPlannedPush((rs.getTimestamp("next_planned_push").toLocalDateTime()))
                    .build();
        }
    }

}
