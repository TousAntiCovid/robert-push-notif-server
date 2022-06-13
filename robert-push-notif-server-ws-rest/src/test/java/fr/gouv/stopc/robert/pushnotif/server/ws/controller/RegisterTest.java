package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.server.api.model.PushRequest;
import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.test.IntegrationTest;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static fr.gouv.stopc.robert.pushnotif.server.ws.test.InstantInAcceptedRangeMatcher.isTimeBetween8amAnd7Pm;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.PsqlManager.*;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.RestAssuredManager.givenBaseHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
public class RegisterTest {

    @RepeatedTest(1000)
    public void created_when_new_pushToken_is_sent() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(CREATED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("locale", is("fr-FR")),
                                        hasProperty("timezone", is("Europe/Paris"))
                                ),
                                allOf(
                                        hasProperty("token", is("OtherPushToken")),
                                        hasProperty("locale", is("fr-FR")),
                                        hasProperty("timezone", is("Europe/Paris"))
                                )
                        )
                )
        );
    }

    @RepeatedTest(1000)
    public void created_with_zone_offset_11_has_nextPushDate_setup_between_7pm_and_6am_next_day_utc() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .locale("en-EN")
                                .timezone("Pacific/Kosrae")
                                .build()
                )
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(CREATED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("locale", is("fr-FR")),
                                        hasProperty("timezone", is("Europe/Paris"))
                                ),
                                allOf(
                                        hasProperty("token", is("OtherPushToken")),
                                        hasProperty("locale", is("en-EN")),
                                        hasProperty("timezone", is("Pacific/Kosrae")),
                                        hasProperty("nextPlannedPush", isTimeBetween8amAnd7Pm("Pacific/Kosrae"))

                                )
                        )
                )
        );
    }

    @RepeatedTest(1000)
    public void created_and_activated_when_already_existing_inactive_token_is_sent() {

        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .token("PushToken")
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .deleted(false)
                        .active(false)
                        .nextPlannedPush(defaultNextPlannedPushDate)
                        .build()
        );
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("PushToken")
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(CREATED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("active", is(true)),
                                        hasProperty("nextPlannedPush", isTimeBetween8amAnd7Pm("Europe/Paris"))
                                )
                        )
                )
        );
    }

    @RepeatedTest(1000)
    public void created_and_activated_when_already_existing_deleted_token_is_sent() {

        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .token("PushToken")
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .deleted(true)
                        .active(false)
                        .nextPlannedPush(defaultNextPlannedPushDate)
                        .build()
        );
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("PushToken")
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(CREATED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("deleted", equalTo(false)),
                                        hasProperty("active", equalTo(true)),
                                        hasProperty("nextPlannedPush", isTimeBetween8amAnd7Pm("Europe/Paris"))
                                )
                        )
                )
        );
    }

    @RepeatedTest(1000)
    public void created_and_activated_when_updating_timezone() {

        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .token("PushToken")
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .deleted(true)
                        .active(false)
                        .nextPlannedPush(defaultNextPlannedPushDate)
                        .build()
        );
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("PushToken")
                                .locale("fr-FR")
                                .timezone("Pacific/Auckland")
                                .build()
                )
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(CREATED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("deleted", equalTo(false)),
                                        hasProperty("active", equalTo(true)),
                                        hasProperty("nextPlannedPush", isTimeBetween8amAnd7Pm("Pacific/Auckland"))
                                )
                        )
                )
        );
    }

    @RepeatedTest(1000)
    public void created_when_already_registered_but_with_different_values() {

        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("PushToken")
                                .locale("en-EN")
                                .timezone("Europe/London")
                                .build()
                )
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(CREATED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("locale", is("en-EN")),
                                        hasProperty("timezone", is("Europe/London")),
                                        hasProperty("nextPlannedPush", isTimeBetween8amAnd7Pm("Europe/Paris"))
                                )
                        )
                )
        );
    }

    @Test
    public void method_not_allowed_when_using_get_method() {
        givenOneFrPushInfoWith("PushToken");

        givenBaseHeaders()
                .when()
                .get("/internal/api/v1/push-token")
                .then()
                .statusCode(METHOD_NOT_ALLOWED.value())
                .body(is(emptyOrNullString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_body_is_empty() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body(is(emptyOrNullString()));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_token_is_null() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("token", is("must not be null"));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_token_is_an_empty_string() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("")
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("token", is("size must be between 1 and 2147483647"));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_locale_is_null() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("locale", is("must not be null"));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_locale_is_an_empty_string() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .locale("")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("locale", is("size must be between 1 and 2147483647"));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_timezone_is_null() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("timezone", is("must not be null"));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_timezone_is_an_empty_string() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .timezone("")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("timezone", is("size must be between 1 and 2147483647"));
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }

    @Test
    public void bad_request_when_timezone_is_invalid() {
        givenOneFrPushInfoWith("PushToken");

        givenBaseHeaders()
                .body(
                        PushRequest.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .timezone("Europe/Invalid")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value());
        assertThat(
                getPushInfos(), allOf(
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("timezone", is("Europe/Paris")),
                                        hasProperty("nextPlannedPush", is(defaultNextPlannedPushDate))
                                )
                        )
                )
        );
    }
}
