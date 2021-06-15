package test.fr.gouv.stopc.robert.pushnotif.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.util.UriComponentsBuilder;

import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import fr.gouv.stopc.robert.pushnotif.server.ws.RobertPushNotifWsRestApplication;
import fr.gouv.stopc.robert.pushnotif.server.ws.utils.UriConstants;
import fr.gouv.stopc.robert.pushnotif.server.ws.vo.PushInfoVo;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { RobertPushNotifWsRestApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application.properties")
public class RegisterPushNotificationControllerWsRestTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private IPushInfoService pushInfoService;

    @Value("${controller.path.prefix}" + UriConstants.API_V1)
    private String pathPrefix;

    private URI targetURI;

    private HttpHeaders headers;

    @BeforeEach
    public void setup() {

        this.targetURI = UriComponentsBuilder.fromUriString(this.pathPrefix).path(UriConstants.PATH).build().encode().toUri();

        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void testRegisterWhenUsingGetMethod() {

        // When
        ResponseEntity<Object> response = this.testRestTemplate.exchange(targetURI, HttpMethod.GET, new HttpEntity<>(null, this.headers),
                Object.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    public void testRegisterWhenPushInfoIsNull() {

        // When
        ResponseEntity<Object> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(null, this.headers), Object.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void testRegisterWhenPushInfoTokenIsNull() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .timezone("Europe/Paris")
                .locale("fr-FR")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Token is mandatory"));
    }

    @Test
    public void testRegisterWhenPushInfoTokenIsBlank() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("")
                .timezone("Europe/Paris")
                .locale("fr-FR")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Token is mandatory"));
    }

    @Test
    public void testRegisterWhenPushInfoLocaleIsNull() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("PushToken")
                .timezone("Europe/Paris")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Locale is mandatory"));
    }

    @Test
    public void testRegisterWhenPushInfoLocaleIsBlank() {

        // Given
        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("PushToken")
                .timezone("Europe/Paris")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Locale is mandatory"));
    }

    @Test
    public void testRegisterWhenPushInfoTimezoneIsNull() {

        // Given
        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("PushToken")
                .locale("fr-FR")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Timezone is mandatory"));
    }

    @Test
    public void testRegisterWhenPushInfoTimezoneIsBlank() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("PushToken")
                .locale("fr-FR")
                .timezone("")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Timezone is mandatory"));
    }

    @Test
    public void testRegisterWhenPushInfoTimezoneIsInvalid() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("PushToken")
                .locale("fr-FR")
                .timezone("Europe/Invalid")
                .build();

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(this.pushInfoService).findByPushToken("PushToken");
        verify(this.pushInfoService, never()).createOrUpdate(any(PushInfo.class));
    }

    @Test
    public void testRegisterSucceedsWhenNotAlreadyRegistered() {

        // Given
        PushInfoVo pushInfo = PushInfoVo.builder()
                .token("PushToken")
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .build();

        when(this.pushInfoService.findByPushToken(pushInfo.getToken())).thenReturn(Optional.empty());

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(this.pushInfoService).findByPushToken("PushToken");
        verify(this.pushInfoService).createOrUpdate(any(PushInfo.class));
    }

    @Test
    public void testRegisterSucceedsWhenAlreadyRegisteredButInactive() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("PushToken")
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .build();

        Date nextPlannedPushed  = TimeUtils.getNowZoneUTC();

        PushInfo push = PushInfo.builder()
                .token(pushInfoVo.getToken())
                .locale(pushInfoVo.getLocale())
                .timezone(pushInfoVo.getTimezone())
                .active(false)
                .deleted(false)
                .nextPlannedPush(nextPlannedPushed)
                .build();

        when(this.pushInfoService.findByPushToken(pushInfoVo.getToken()))
        .thenReturn(Optional.of(push));

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(this.pushInfoService).findByPushToken("PushToken");
        verify(this.pushInfoService).createOrUpdate(push);
        assertTrue(push.isActive());
        assertNotEquals(nextPlannedPushed, push.getNextPlannedPush());
    }

    @Test
    public void testRegisterSucceedsWhenAlreadyRegisteredButDeleted() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("PushToken")
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .build();

        Date nextPlannedPushed  = TimeUtils.getNowZoneUTC();

        PushInfo push = PushInfo.builder()
                .token(pushInfoVo.getToken())
                .locale(pushInfoVo.getLocale())
                .timezone(pushInfoVo.getTimezone())
                .active(false)
                .deleted(true)
                .nextPlannedPush(nextPlannedPushed)
                .build();

        when(this.pushInfoService.findByPushToken(pushInfoVo.getToken()))
        .thenReturn(Optional.of(push));

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(this.pushInfoService).findByPushToken("PushToken");
        verify(this.pushInfoService).createOrUpdate(push);
        assertTrue(push.isActive());
        assertFalse(push.isDeleted());
        assertNotEquals(nextPlannedPushed, push.getNextPlannedPush());
    }

    @Test
    public void testRegisterSucceedsWhenAlreadyRegisteredButDifferentsValues() {

        // Given
        PushInfoVo pushInfoVo = PushInfoVo.builder()
                .token("PushToken")
                .locale("en-EN")
                .timezone("Europe/London")
                .build();

        Date nextPlannedPushed  = TimeUtils.getNowZoneUTC();

        PushInfo push = PushInfo.builder()
                .token(pushInfoVo.getToken())
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .active(true)
                .deleted(false)
                .nextPlannedPush(nextPlannedPushed)
                .build();

        when(this.pushInfoService.findByPushToken(pushInfoVo.getToken()))
        .thenReturn(Optional.of(push));

        // When
        ResponseEntity<String> response = this.testRestTemplate.exchange(targetURI, HttpMethod.POST,
                new HttpEntity<>(pushInfoVo, this.headers), String.class);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(this.pushInfoService).findByPushToken("PushToken");
        verify(this.pushInfoService).createOrUpdate(push);
        assertEquals("en-EN", push.getLocale());
        assertEquals("Europe/London", push.getTimezone());
        assertEquals(nextPlannedPushed, push.getNextPlannedPush());
    }
}
