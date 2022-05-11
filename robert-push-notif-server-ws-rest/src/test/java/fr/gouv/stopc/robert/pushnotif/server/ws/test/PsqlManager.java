package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PsqlManager implements TestExecutionListener {

    private static final JdbcDatabaseContainer POSTGRE = new PostgreSQLContainer(DockerImageName.parse("postgres:9.6"));

    private JdbcTemplate jdbcTemplate;

    static {
        POSTGRE.start();
        System.setProperty("spring.datasource.url", POSTGRE.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRE.getUsername());
        System.setProperty("spring.datasource.password", POSTGRE.getPassword());
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        jdbcTemplate = testContext.getApplicationContext().getBean(JdbcTemplate.class);
        jdbcTemplate.execute("DROP SCHEMA public CASCADE ;");
        jdbcTemplate.execute("CREATE SCHEMA public ;");
    }
}
