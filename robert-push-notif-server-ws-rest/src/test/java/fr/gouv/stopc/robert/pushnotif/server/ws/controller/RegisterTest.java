package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.test.IntegrationTest;
import fr.gouv.stopc.robert.pushnotif.server.ws.vo.PushInfoVo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils.getNowZoneUTC;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.PsqlManager.*;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.RestAssuredManager.givenBaseHeaders;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
public class RegisterTest {

    Date tomorrowMidnight = Date.from(LocalDate.now().plusDays(1).atStartOfDay(systemDefault()).toInstant());

    @Test
    public void created_when_new_pushToken_is_sent() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(1);
    }

    @Test
    public void created_and_activated_when_already_existing_inactive_token_is_sent() {

        addPushToken(
                PushInfo.builder()
                        .token("PushToken")
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .deleted(false)
                        .active(false)
                        .nextPlannedPush(getNowZoneUTC())
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
        assertThat(getPushInfoByToken("PushToken"))
                .extracting(
                        PushInfo::isActive,
                        PushInfo::getNextPlannedPush
                )
                .containsExactly(
                        true,
                        tomorrowMidnight
                );
    }

    @Test
    public void created_and_activated_when_already_existing_deleted_token_is_sent() {

        addPushToken(
                PushInfo.builder()
                        .token("PushToken")
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .deleted(true)
                        .active(true)
                        .nextPlannedPush(getNowZoneUTC())
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
        assertThat(getPushInfoByToken("PushToken"))
                .extracting(
                        PushInfo::isDeleted,
                        PushInfo::getNextPlannedPush
                )
                .containsExactly(
                        false,
                        tomorrowMidnight
                );
    }

    @Test
    public void created_when_already_registered_but_with_different_values() {

        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
        assertThat(getPushInfoByToken("PushToken"))
                .extracting(
                        PushInfo::getLocale,
                        PushInfo::getTimezone,
                        PushInfo::getNextPlannedPush
                )
                .containsExactly(
                        "en-EN",
                        "Europe/London",
                        tomorrowMidnight
                );
    }

    @Test
    public void method_not_allowed_when_using_get_method() {
        loadOneFrPushToken("PushToken");
        givenBaseHeaders()
                .when()
                .get("/internal/api/v1/push-token")
                .then()
                .statusCode(METHOD_NOT_ALLOWED.value())
                .body(is(emptyOrNullString()));
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_body_is_empty() {
        loadOneFrPushToken("PushToken");
        givenBaseHeaders()
                .when()
                .post("/internal/api/v1/push-token")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body(is(emptyOrNullString()));
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_token_is_null() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_token_is_an_empty_string() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_locale_is_null() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_locale_is_an_empty_string() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_timezone_is_null() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_timezone_is_an_empty_string() {
        loadOneFrPushToken("PushToken");
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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }

    @Test
    public void bad_request_when_timezone_is_invalid() {
        loadOneFrPushToken("PushToken");

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
        assertThat(pushInfosCountDifferenceSinceLastUpdate()).isEqualTo(0);
    }
}
