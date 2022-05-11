package fr.gouv.stopc.robert.pushnotif.server.ws.test;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

public class RestAssuredManager implements TestExecutionListener {

    public static RequestSpecification givenBaseHeaders() {
        return given()
                .accept(JSON)
                .contentType(JSON);
    }

    private static void configureRestAssured(final Integer port) {
        RestAssured.port = port;
        enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        configureRestAssured(
                testContext.getApplicationContext().getEnvironment()
                        .getRequiredProperty("local.server.port", Integer.class)
        );
    }
}
