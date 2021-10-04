package test.fr.gouv.stopc.robert.pushnotif.batch.it;

import fr.gouv.stopc.robert.pushnotif.batch.RobertPushNotifBatchApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@ActiveProfiles("dev")
@SpringBootTest(classes = { RobertPushNotifBatchApplication.class })
@TestExecutionListeners(listeners = { APNsServersManager.class,
        PostgreSqlManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
@Retention(RUNTIME)
@Target(TYPE)
@Sql(scripts = {
        "/init.sql" }, config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED))
public @interface IntegrationTest {
}