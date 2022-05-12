package test.fr.gouv.stopc.robert.pushnotif.ws;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import fr.gouv.stopc.robert.pushnotif.server.ws.RobertPushNotifWsRestApplication;
import fr.gouv.stopc.robert.pushnotif.server.ws.utils.UriConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
        RobertPushNotifWsRestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class UnregisterPushNotificationControllerWsRest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private IPushInfoService pushInfoService;

    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefix;

    private URI targetURI;

    private String token = "pushToken";

    @BeforeEach
    public void setup() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("token", token);

        this.targetURI = UriComponentsBuilder.fromUriString(this.pathPrefix)
                .path(UriConstants.PATH)
                .path(UriConstants.TOKEN_PATH_VARIABLE)
                .build(parameters);

    }

    @Test
    public void testRegisterWhenUsingGetMethod() {

        // When
        ResponseEntity<Object> response = this.testRestTemplate.exchange(
                targetURI, HttpMethod.GET, new HttpEntity<>(null),
                Object.class
        );

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    public void testRegisterWhenTokenIsNotProvided() {

        // Given
        this.targetURI = UriComponentsBuilder.fromUriString(this.pathPrefix)
                .path(UriConstants.PATH)
                .build()
                .toUri();

        // When
        ResponseEntity<Object> response = this.testRestTemplate.exchange(
                targetURI, HttpMethod.DELETE, null,
                Object.class
        );

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    public void testRegisterWhenTokenNotFound() {

        // Given
        when(this.pushInfoService.findByPushToken(this.token)).thenReturn(Optional.empty());

        // When
        ResponseEntity<Object> response = this.testRestTemplate.exchange(
                targetURI, HttpMethod.DELETE, null,
                Object.class
        );

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testRegisterWhenTokenExists() {

        // Given
        PushInfo push = PushInfo.builder()
                .token(this.token)
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .active(false)
                .deleted(false)
                .nextPlannedPush(new Date())
                .build();
        when(this.pushInfoService.findByPushToken(this.token)).thenReturn(Optional.of(push));

        // When
        ResponseEntity<Object> response = this.testRestTemplate.exchange(
                targetURI, HttpMethod.DELETE, null,
                Object.class
        );

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertTrue(push.isDeleted());
    }
}
