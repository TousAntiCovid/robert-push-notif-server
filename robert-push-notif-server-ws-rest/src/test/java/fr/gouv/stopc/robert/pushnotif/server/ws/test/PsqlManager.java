package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils.getNowZoneUTC;

public class PsqlManager implements TestExecutionListener {

    static JdbcTemplate jdbcTemplate;

    private static final JdbcDatabaseContainer POSTGRE = new PostgreSQLContainer(DockerImageName.parse("postgres:9.6"));

    private static Integer lastPushInfosDatatableCount = 0;

    static {
        POSTGRE.start();
        System.setProperty("spring.datasource.url", POSTGRE.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRE.getUsername());
        System.setProperty("spring.datasource.password", POSTGRE.getPassword());
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        jdbcTemplate.execute("drop schema public cascade ;");
        jdbcTemplate.execute("drop table if exists push cascade ;");
        jdbcTemplate.execute("create schema public ;");
        jdbcTemplate.execute(
                "create table push (id  bigserial not null, active boolean, creation_date timestamp, deleted boolean, failed_push_sent int4, last_error_code varchar(255), last_failure_push timestamp, last_successful_push timestamp, locale varchar(255) not null, next_planned_push timestamp, successful_push_sent int4, timezone varchar(255) not null, token varchar(255) not null, primary key (id));"
        );
        jdbcTemplate.execute("create index IDX_TOKEN on push (token);");
        jdbcTemplate.execute("alter table push add constraint UK_aw1ibs9m7eqsdmrin9p3b37i1 unique (token);");
        lastPushInfosDatatableCount = countPushInfos();
    }

    @Override
    public void beforeTestClass(TestContext testContext) {
        jdbcTemplate = testContext.getApplicationContext().getBean(JdbcTemplate.class);
    }

    public static void loadOneFrPushToken(String token) {
        addPushToken(
                PushInfo.builder()
                        .token(token)
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .nextPlannedPush(getNowZoneUTC())
                        .build()
        );
        lastPushInfosDatatableCount = countPushInfos();
    }

    public static void disableToken(String token) {
        final String query = "update push set active = ? where token = ?";
        jdbcTemplate.update(query, false, token);
    }

    public static int pushInfosCountDifferenceSinceLastUpdate() {
        final Integer newCount = countPushInfos();
        if (newCount > lastPushInfosDatatableCount) {
            return newCount - lastPushInfosDatatableCount;
        } else {
            return lastPushInfosDatatableCount - newCount;
        }
    }

    public static void addPushToken(PushInfo pushInfo) {
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
        lastPushInfosDatatableCount = countPushInfos();
    }

    public static PushInfo getPushInfoByToken(String token) {
        return jdbcTemplate.queryForObject("select * from push where token = '" + token + "'", new PushInfoRowMapper());
    }

    public static Integer countPushInfos() {
        return jdbcTemplate.queryForObject("select count(*) from push", Integer.class);
    }
}
