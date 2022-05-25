package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Retention(RUNTIME)
@TestExecutionListeners(listeners = { RestAssuredManager.class, PsqlManager.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public @interface IntegrationTest {
}
