package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.test.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static fr.gouv.stopc.robert.pushnotif.server.ws.test.PsqlManager.*;
import static fr.gouv.stopc.robert.pushnotif.server.ws.test.RestAssuredManager.givenBaseHeaders;
import static java.time.LocalDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@IntegrationTest
public class UnregisterTest {

    @Test
    public void existing_pushtoken_is_deleted() {
        givenOneFrPushInfoWith("PushToken");
        givenBaseHeaders()
                .delete("/internal/api/v1/push-token/" + "PushToken")
                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(),
                contains(
                        allOf(
                                hasProperty("token", is("PushToken")),
                                hasProperty("deleted", is(true))
                        )
                )
        );
    }

    @Test
    public void existing_already_deleted_pushtoken_is_still_deleted() {
        givenOnePushInfoSuchAs(
                PushInfo.builder()
                        .token("PushToken")
                        .locale("fr-FR")
                        .timezone("Europe/Paris")
                        .deleted(true)
                        .active(false)
                        .nextPlannedPush(now().toInstant(ZoneOffset.UTC))
                        .build()
        );
        givenBaseHeaders()
                .delete("/internal/api/v1/push-token/" + "PushToken")
                .then()
                .statusCode(ACCEPTED.value())
                .body(is(emptyString()));
        assertThat(
                getPushInfos(),
                contains(
                        hasProperty("deleted", is(true))
                )
        );
    }

    @Test
    public void non_existing_pushtoken_is_not_deleted() {
        givenBaseHeaders()
                .delete("/internal/api/v1/push-token/UnexistingToken")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body(is(emptyOrNullString()));
        assertThat(getPushInfos(), hasSize(0));
    }
}
