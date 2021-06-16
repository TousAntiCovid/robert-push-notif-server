package test.fr.gouv.stopc.robert.pushnotif.batch.rest.service.impl;

import fr.gouv.stopc.robert.pushnotif.batch.rest.dto.NotificationDetailsDto;
import fr.gouv.stopc.robert.pushnotif.batch.rest.service.IRestApiService;
import fr.gouv.stopc.robert.pushnotif.batch.rest.service.impl.RestApiServiceImpl;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
public class RestApiServiceImplTest {

    @InjectMocks
    private RestApiServiceImpl restApiSerice;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private IRestApiService restApiService;

    @Mock
    private PropertyLoader propertyLoader;

    @Value("${robert.push.notif.server.notification.available-languages}")
    private String[] availableNotificationLanguages;

    @Value("${robert.push.notif.server.notification.url}")
    private String notificationContentUrl;

    @Value("${robert.push.notif.server.notification.url.version}")
    private String notificationContentUrlVersion;

    @BeforeEach
    public void beforeEach() {

        when(this.propertyLoader.getAvailableNotificationLanguages()).thenReturn(availableNotificationLanguages);
        when(this.propertyLoader.getNotificationContentUrl()).thenReturn(notificationContentUrl);
        when(this.propertyLoader.getNotificationContentUrlVersion()).thenReturn(notificationContentUrlVersion);
    }

    @Test
    public void testGetNotificationDetailsWhenLocaleIsNull() {

        // Given
        String locale = null;

        // When
        Optional<NotificationDetailsDto> notifDetails = this.restApiSerice.getNotificationDetails(locale);

        // Then
        assertFalse(notifDetails.isPresent());
        verify(this.restTemplate, never()).getForEntity(any(URI.class), any());
    }

    @Test
    public void testGetNotificationDetailsWhenLocaleIsEmpty() {

        // Given
        String locale = " ";

        // When
        Optional<NotificationDetailsDto> notifDetails = this.restApiSerice.getNotificationDetails(locale);

        // Then
        assertFalse(notifDetails.isPresent());
        verify(this.restTemplate, never()).getForEntity(any(URI.class), any());
    }

    @Test
    public void testGetNotificationDetailsWhenLocaleHasBadLength() {

        // Given
        String locale = "f";

        // When
        Optional<NotificationDetailsDto> notifDetails = this.restApiSerice.getNotificationDetails(locale);

        // Then
        assertFalse(notifDetails.isPresent());
        verify(this.restTemplate, never()).getForEntity(any(URI.class), any());
    }

    @Test
    public void testGetNotificationDetailsWhenLocaleIsNotKnown() {

        // Given
        String locale = "fg-FR";

        // When
        Optional<NotificationDetailsDto> notifDetails = this.restApiSerice.getNotificationDetails(locale);

        // Then
        assertFalse(notifDetails.isPresent());
        verify(this.restTemplate, never()).getForEntity(any(URI.class), any());
    }

    @Test
    public void testGetNotificationDetailsWhenLocaleWhenAnExceptionIsThrown() {

        // Given
        String locale = "fr-FR";

        when(this.restTemplate.getForEntity(any(URI.class), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // When
        Optional<NotificationDetailsDto> notifDetails = this.restApiSerice.getNotificationDetails(locale);

        // Then
        assertFalse(notifDetails.isPresent());
        verify(this.restTemplate).getForEntity(any(URI.class), any());
    }

    @Test
    public void testGetNotificationDetailsWhenLocaleSucceeds() {

        // Given
        String locale = "fr-FR";

        when(this.restTemplate.getForEntity(any(URI.class), any()))
                .thenReturn(
                        ResponseEntity.ok(
                                NotificationDetailsDto.builder().title("Hello").message("Merci").build()
                        )
                );

        // When
        Optional<NotificationDetailsDto> notifDetails = this.restApiSerice.getNotificationDetails(locale);

        // Then
        assertTrue(notifDetails.isPresent());
        verify(this.restTemplate).getForEntity(any(URI.class), any());
    }
}
