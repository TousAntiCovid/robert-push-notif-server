package test.fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl.ApnsPushNotificationServiceImpl;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

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
    public void testSendPushNotificationWhenAnExceptionIsthrown() {

        // Given
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        sendNotificationFuture = new PushNotificationFuture<>(null);

        String runtimeExceptionMessage = "failed";

        sendNotificationFuture.completeExceptionally(new RuntimeException(runtimeExceptionMessage));

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture);

        assertNull(this.push.getLastFailurePush());
        assertNull(this.push.getNextPlannedPush());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        this.pause(1);

        // Then
        assertNotNull(this.push.getLastFailurePush());
        assertNotNull(this.push.getNextPlannedPush());
        assertEquals(1, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());
        assertNotNull(this.push.getLastErrorCode());
        assertTrue(this.push.getLastErrorCode().contains(runtimeExceptionMessage));
        verify(this.apnsClient).sendNotification(any(SimpleApnsPushNotification.class));

    }

    @Test
    public void testSendPushNotificationFailsWithInactiveRejectionReason() {

        // Given
        String rejectionReason = "BadDeviceToken";

        // When & Then
        this.testSendPushNotificationFails(false, rejectionReason, 1);

    }

    @Test
    public void testSendPushNotificationFailsWithInactiveRejectionReasonAndSecondaryPushEnabled() {

        // Given
        String rejectionReason = "BadDeviceToken";

        ReflectionTestUtils.setField(this.apnsService, "secondaryApnsClient", this.apnsClient);

        when(this.propertyLoader.isEnableSecondaryPush()).thenReturn(true);

        // When & Then
        this.testSendPushNotificationFails(false, rejectionReason, 2);

    }

    @Test
    public void testSendPushNotificationFailsWithInactiveRejectionReasonAndCall() {

        // Given
        String rejectionReason = "BadDeviceToken";

        // When & Then
        this.testSendPushNotificationFails(false, rejectionReason, 1);

    }

    @Test
    public void testSendPushNotificationFailsWithAnotherRejectionReason() {

        // Given
        String rejectionReason = "DeviceTokenNotForTopic";

        // When & Then
        this.testSendPushNotificationFails(true, rejectionReason, 1);

    }

    @Test
    public void testSendPushNotificationFailsWithSecondaryPushEnabled() {

        // Given
        String rejectionReason = "BadDeviceToken";
        String nextRejectionReason = "DeviceTokenNotForTopic";

        ReflectionTestUtils.setField(this.apnsService, "secondaryApnsClient", this.apnsClient);

        when(this.propertyLoader.isEnableSecondaryPush()).thenReturn(true);
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        sendNotificationFuture = new PushNotificationFuture<>(null);

        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        nextSendNotificationFuture = new PushNotificationFuture<>(null);

        PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = this.getPushNotificationResponse(false, rejectionReason);

        PushNotificationResponse<SimpleApnsPushNotification> nextPushNotificationResponse = this.getPushNotificationResponse(false, nextRejectionReason);

        sendNotificationFuture.complete(pushNotificationResponse);
        nextSendNotificationFuture.complete(nextPushNotificationResponse);

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture)
        .thenReturn(nextSendNotificationFuture);

        assertNull(this.push.getLastFailurePush());
        assertNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        this.pause(1);

        // Then
        assertTrue(this.push.isActive());
        assertNotNull(this.push.getLastFailurePush());
        assertNull(this.push.getLastSuccessfulPush());
        assertNotNull(this.push.getNextPlannedPush());
        assertEquals(nextRejectionReason, this.push.getLastErrorCode());
        assertEquals(1, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());
        verify(this.apnsClient, times(2)).sendNotification(any(SimpleApnsPushNotification.class));

    }

    @Test
    public void testSendPushNotificationSucceedsWithOnlyOneInactiveRejectionReasonAndSecondaryPushEnabled() {


        // Given
        String rejectionReason = "BadDeviceToken";
        String nextRejectionReason = null;

        ReflectionTestUtils.setField(this.apnsService, "secondaryApnsClient", this.apnsClient);

        when(this.propertyLoader.isEnableSecondaryPush()).thenReturn(true);
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        sendNotificationFuture = new PushNotificationFuture<>(null);

        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        nextSendNotificationFuture = new PushNotificationFuture<>(null);

        PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = this.getPushNotificationResponse(false, rejectionReason);

        PushNotificationResponse<SimpleApnsPushNotification> nextPushNotificationResponse = this.getPushNotificationResponse(true, nextRejectionReason);

        sendNotificationFuture.complete(pushNotificationResponse);
        nextSendNotificationFuture.complete(nextPushNotificationResponse);

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture)
        .thenReturn(nextSendNotificationFuture);

        assertNull(this.push.getLastFailurePush());
        assertNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        this.pause(1);

        // Then
        assertTrue(this.push.isActive());
        assertNull(this.push.getLastFailurePush());
        assertNotNull(this.push.getLastSuccessfulPush());
        assertNotNull(this.push.getNextPlannedPush());
        assertEquals(null, this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(1, this.push.getSuccessfulPushSent());
        verify(this.apnsClient, times(2)).sendNotification(any(SimpleApnsPushNotification.class));

    }

    @Test
    public void testSendPushNotificationWhenItSucceeds() {

        // Given
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        sendNotificationFuture = new PushNotificationFuture<>(null);

        PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = this.getPushNotificationResponse(true, null);

        sendNotificationFuture.complete(pushNotificationResponse);

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture);

        assertNull(this.push.getLastFailurePush());
        assertNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        this.pause(1);

        // Then
        assertTrue(this.push.isActive());
        assertNotNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(1, this.push.getSuccessfulPushSent());
        verify(this.apnsClient).sendNotification(any(SimpleApnsPushNotification.class));

    }

    private void testSendPushNotificationFails(boolean isActive, String rejectionReason, int times) {

        // Given
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
        sendNotificationFuture = new PushNotificationFuture<>(null);

        PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = this.getPushNotificationResponse(false, rejectionReason);


        sendNotificationFuture.complete(pushNotificationResponse);

        when(this.apnsClient.sendNotification(any(SimpleApnsPushNotification.class))).thenReturn(sendNotificationFuture);

        assertTrue(this.push.isActive());
        assertNull(this.push.getLastFailurePush());
        assertNull(this.push.getNextPlannedPush());
        assertNull(this.push.getLastErrorCode());
        assertEquals(0, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());

        // When
        this.apnsService.sendPushNotification(this.push);

        this.pause(1);

        // Then
        assertEquals(isActive, this.push.isActive());
        assertNotNull(this.push.getLastFailurePush());
        assertNull(this.push.getLastSuccessfulPush());
        assertNotNull(this.push.getNextPlannedPush());
        assertEquals(rejectionReason, this.push.getLastErrorCode());
        assertEquals(1, this.push.getFailedPushSent());
        assertEquals(0, this.push.getSuccessfulPushSent());
        verify(this.apnsClient, times(times)).sendNotification(any(SimpleApnsPushNotification.class));

    }

    private PushNotificationResponse<SimpleApnsPushNotification> getPushNotificationResponse(boolean isAccepted, String rejectionReason) {
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
                return rejectionReason;
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

    private void pause(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}
