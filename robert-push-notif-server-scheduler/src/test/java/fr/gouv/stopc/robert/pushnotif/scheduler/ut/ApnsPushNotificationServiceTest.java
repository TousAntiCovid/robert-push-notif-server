package fr.gouv.stopc.robert.pushnotif.scheduler.ut;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientDefinition;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsDefinition;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ApnsPushNotificationServiceTest {

    @Mock
    private PushInfoDao pushInfoDao;

    @Mock
    private ApnsClientFactory apnsClientFactory;

    ApnsPushNotificationService fastService;

    ApnsPushNotificationService slowService;

    @BeforeEach
    public void init() {

        PropertyLoader propertyLoaderWithFastNotificationSending = PropertyLoader.builder()
                .rateLimitingCapacity(1)
                .rateLimitingRefillDurationInSec(1)
                .maxNumberOfOutstandingNotification(50)
                .apns(
                        ApnsDefinition.builder()
                                .authKeyId("key-id")
                                .topic("topic")
                                .clients(
                                        Collections.singletonList(
                                                ApnsClientDefinition.builder().host("localhost").port(443).build()
                                        )
                                )
                                .build()
                )
                .build();

        PropertyLoader propertyLoaderWithSlowNotificationSending = PropertyLoader.builder()
                .rateLimitingCapacity(1)
                .rateLimitingRefillDurationInSec(2)
                .maxNumberOfOutstandingNotification(50)
                .apns(
                        ApnsDefinition.builder()
                                .authKeyId("key-id")
                                .topic("topic")
                                .clients(
                                        Collections.singletonList(
                                                ApnsClientDefinition.builder().host("localhost").port(443).build()
                                        )
                                )
                                .build()
                )
                .build();

        fastService = new ApnsPushNotificationService(
                propertyLoaderWithFastNotificationSending, pushInfoDao, apnsClientFactory
        );
        slowService = new ApnsPushNotificationService(
                propertyLoaderWithSlowNotificationSending, pushInfoDao, apnsClientFactory
        );
    }

    @Test
    public void shouldTakeIntoAccountRateLimiting() {

        PushInfo pushInfo = PushInfo.builder().token("123456789").build();

        long currentTimeBeforeSlowSendingNotif = System.currentTimeMillis();
        IntStream.range(0, 3).forEach(i -> slowService.sendPushNotification(pushInfo));
        long currentTimeAfterSlowSendingNotif = System.currentTimeMillis();

        long durationSlowSendingNotif = currentTimeAfterSlowSendingNotif - currentTimeBeforeSlowSendingNotif;

        long currentTimeBeforeFastSendingNotif = System.currentTimeMillis();
        IntStream.range(0, 3).forEach(i -> fastService.sendPushNotification(pushInfo));
        long currentTimeAfterFastSendingNotif = System.currentTimeMillis();

        long durationFastSendingNotif = currentTimeAfterFastSendingNotif - currentTimeBeforeFastSendingNotif;

        assertThat(durationSlowSendingNotif).isCloseTo(2 * durationFastSendingNotif, Percentage.withPercentage(5));

    }

}
