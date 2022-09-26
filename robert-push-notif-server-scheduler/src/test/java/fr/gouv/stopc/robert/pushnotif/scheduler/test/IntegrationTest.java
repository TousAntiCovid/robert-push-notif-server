package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@Target(TYPE)
@DirtiesContext
@Retention(RUNTIME)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles({ "test" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestExecutionListeners(listeners = { APNsMockServersManager.class, PsqlManager.class,
        MetricsManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
public @interface IntegrationTest {
}
