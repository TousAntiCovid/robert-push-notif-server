package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.server.ws.vo.PushInfoVo;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static fr.gouv.stopc.robert.pushnotif.server.ws.test.DateInAcceptedRangeMatcher.isLocalTimeBetween8amAnd7pm;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.PsqlManager.*;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.RestAssuredManager.givenBaseHeaders;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
public class RegisterTest {

    @Test
    public void created_when_new_pushToken_is_sent() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .body(
                        PushInfoVo.builder()
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
                        hasSize(2),
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
                        PushInfoVo.builder()
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
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("active", is(true)),
                                        hasProperty("nextPlannedPush", isLocalTimeBetween8amAnd7pm())
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
                        PushInfoVo.builder()
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
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("deleted", equalTo(false)),
                                        hasProperty("active", equalTo(true)),
                                        hasProperty("nextPlannedPush", isLocalTimeBetween8amAnd7pm())
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
                        PushInfoVo.builder()
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
                        hasSize(1),
                        contains(
                                allOf(
                                        hasProperty("token", is("PushToken")),
                                        hasProperty("locale", is("en-EN")),
                                        hasProperty("timezone", is("Europe/London")),
                                        hasProperty("nextPlannedPush", isLocalTimeBetween8amAnd7pm())
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
                        hasSize(1),
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
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("token", is("Token is mandatory"));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .token("")
                                .locale("fr-FR")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("token", is("Token is mandatory"));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .token("OtherPushToken")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("locale", is("Locale is mandatory"));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .token("OtherPushToken")
                                .locale("")
                                .timezone("Europe/Paris")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("locale", is("Locale is mandatory"));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("timezone", is("Timezone is mandatory"));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .timezone("")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("timezone", is("Timezone is mandatory"));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
                        PushInfoVo.builder()
                                .token("OtherPushToken")
                                .locale("fr-FR")
                                .timezone("Europe/Invalid")
                                .build()
                )
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body(is(emptyOrNullString()));
        assertThat(
                getPushInfos(), allOf(
                        hasSize(1),
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
