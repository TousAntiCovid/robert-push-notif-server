package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.repository.PushInfoRepository;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class PsqlManager implements TestExecutionListener {

    static PushInfoRepository pushInfoRepository;

    private static final JdbcDatabaseContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:9.6")
    );

    public static Instant defaultNextPlannedPushDate = LocalDateTime.now().toInstant(ZoneOffset.UTC);

    static {
        POSTGRES.start();
        System.setProperty("spring.datasource.url", POSTGRES.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES.getPassword());
    }

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        pushInfoRepository.deleteAll();
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        pushInfoRepository = testContext.getApplicationContext().getBean(PushInfoRepository.class);
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
        pushInfoRepository.save(pushInfo);
    }

    public static List<PushInfo> getPushInfos() {
        return pushInfoRepository.findAll();
    }
}
