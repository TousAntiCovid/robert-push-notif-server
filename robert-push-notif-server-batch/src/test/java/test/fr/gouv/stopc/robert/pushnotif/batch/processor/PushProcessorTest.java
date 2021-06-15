package test.fr.gouv.stopc.robert.pushnotif.batch.processor;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.processor.PushProcessor;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class PushProcessorTest {

    private final static String SHOULD_NOT_FAIL = "Should not fail";

    @InjectMocks
    private PushProcessor pushProcessor;

    @Mock
    private IApnsPushNotificationService apnsPushNotifcationService;

    @Mock
    private PropertyLoader propertyLoader;

    @Test
    public void testProcessWhenNotifsExists() throws Exception {

        // Given
        PushInfo toPush = PushInfo.builder()
                .token("token")
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .build();

        when(propertyLoader.getPushProcessorThrottlingPauseInMs()).thenReturn(0L);

        // When
        this.pushProcessor.process(toPush);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            fail(SHOULD_NOT_FAIL, e);
        }

        // Then
        verify(this.apnsPushNotifcationService).sendPushNotification(toPush);
    }
}
