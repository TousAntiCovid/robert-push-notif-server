package test.fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl.ApnsPushNotificationServiceImpl;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ApnsPushNotificationServiceImplTest {

    @InjectMocks
    private ApnsPushNotificationServiceImpl apnsService;

    @Mock
    private ApnsClient apnsClient;

    @Mock
    private PropertyLoader propertyLoader;

    private PushInfo push;

    @BeforeEach
    public void before() {
        this.push = PushInfo.builder()
                .token("token")
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .active(true)
                .build();

        ReflectionTestUtils.setField(this.apnsService, "apnsClient", this.apnsClient);

        when(this.propertyLoader.getMinPushHour()).thenReturn(8);
        when(this.propertyLoader.getMaxPushHour()).thenReturn(20);
        when(this.propertyLoader.getApnsTopic()).thenReturn("topic");
        when(this.propertyLoader.getApnsInactiveRejectionReason()).thenReturn(Arrays.asList("BadDeviceToken"));
        when(this.propertyLoader.isEnableSecondaryPush()).thenReturn(false);
    }

    @Test
    public void testSendPushNotificationWhenPushInfoIsNull() {

        // When
        this.apnsService.sendPushNotification(null);

        // Then
        verify(this.apnsClient, never()).sendNotification(any(SimpleApnsPushNotification.class));

    }

    @Test
    public void testSendPushNotificationFailsWithSecondaryPushEnabled() {

        // Given
        ReflectionTestUtils.setField(this.apnsService, "secondaryApnsClient", this.apnsClient);

        when(this.propertyLoader.isEnableSecondaryPush()).thenReturn(true);
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                sendNotificationFuture = new PushNotificationFuture<>(null);

        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                nextSendNotificationFuture = new PushNotificationFuture<>(null);

        PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = this.getPushNotificationResponse(false);

        PushNotificationResponse<SimpleApnsPushNotification> nextPushNotificationResponse = this.getPushNotificationResponse(false);

        sendNotificationFuture.complete(pushNotificationResponse);
        nextSendNotificationFuture.complete(nextPushNotificationResponse);

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture)
                .thenReturn(nextSendNotificationFuture);

        assertNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        // Then
        assertTrue(this.push.isActive());
        assertNotNull(this.push.getNextPlannedPush());
        assertEquals(1, this.push.getSuccessfulPushSent());
    }

    @Test
    public void testSendPushNotificationWhenItSucceeds() {

        // Given
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                sendNotificationFuture = new PushNotificationFuture<>(null);

        PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = this.getPushNotificationResponse(true);

        sendNotificationFuture.complete(pushNotificationResponse);

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture);

        assertNull(this.push.getLastFailurePush());
        assertNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        // Then
        assertTrue(this.push.isActive());
        assertNotNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(1, this.push.getSuccessfulPushSent());
        verify(this.apnsClient).sendNotification(any(SimpleApnsPushNotification.class));

    }

    private PushNotificationResponse<SimpleApnsPushNotification> getPushNotificationResponse(boolean isAccepted) {
        return new PushNotificationResponse<SimpleApnsPushNotification>() {

            @Override
            public boolean isAccepted() {
                return isAccepted;
            }

            @Override
            public Optional<Instant> getTokenInvalidationTimestamp() {
                return Optional.empty();
            }

            @Override
            public String getRejectionReason() {
                return null;
            }

            @Override
            public SimpleApnsPushNotification getPushNotification() {
                return null;
            }

            @Override
            public UUID getApnsId() {
                return null;
            }
        };
    }
}
