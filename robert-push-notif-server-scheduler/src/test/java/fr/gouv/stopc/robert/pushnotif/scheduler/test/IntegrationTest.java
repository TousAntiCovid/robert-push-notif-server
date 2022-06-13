package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(listeners = { APNsServersManager.class, PsqlManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
@Retention(RUNTIME)
@Target(TYPE)
public @interface IntegrationTest {
}
