package test.fr.gouv.stopc.robert.pushnotif.batch.processor;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.processor.PushProcessor;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

@ExtendWith(SpringExtension.class)
public class PushProcessorTest {

    private final static String SHOULD_NOT_FAIL = "Should not fail";

    @InjectMocks
    private PushProcessor pushProcessor;

    @Mock
    private IApnsPushNotificationService apnsPushNotifcationService;

    @Test
    public void testProcessWhenNotifsExists() {

        try {
            // Given
            PushInfo toPush = PushInfo.builder()
                    .token("token")
                    .locale("fr-FR")
                    .timezone("Europe/Paris")
                    .build();

            // When
            this.pushProcessor.process(toPush);

            // Then
            verify(this.apnsPushNotifcationService).sendPushNotification(toPush);
        } catch (Exception e) {
            fail(SHOULD_NOT_FAIL);
        }
    }
}
