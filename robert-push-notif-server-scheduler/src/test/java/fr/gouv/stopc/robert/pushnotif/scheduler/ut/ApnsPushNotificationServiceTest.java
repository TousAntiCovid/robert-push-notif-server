package fr.gouv.stopc.robert.pushnotif.scheduler.ut;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.TacApnsClient;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApnsPushNotificationServiceTest {

    @Mock
    private PushInfoDao pushInfoDao;

    @Mock
    private ApnsClientFactory apnsClientFactory;

    @Mock
    private ApnsClient apnsClient;

    ApnsPushNotificationService service;

    @BeforeEach
    public void init() {

        SimpleApnsPushNotification apnsPushNotification = new SimpleApnsPushNotification("token", "topic", "payload");

        PushNotificationFuture apnsAcceptedPushNotifCompletableFuture = new PushNotificationFuture<SimpleApnsPushNotification, PushNotificationFuture>(
                apnsPushNotification
        );
        apnsAcceptedPushNotifCompletableFuture.complete(new AcceptedPushNotificationResponse());

        var apnsClientList = new ArrayList<TacApnsClient>();
        apnsClientList.add(new TacApnsClient(apnsClient, "localhost", 443));

        when(apnsClientFactory.getApnsClients()).thenReturn(apnsClientList);
        when(apnsClient.sendNotification(any())).thenReturn(apnsAcceptedPushNotifCompletableFuture);
    }

    @Test
    public void shouldThrottleNotificationSending() {

        // given
        RobertPushServerProperties robertPushServerProperties = RobertPushServerProperties
                .builder()
                .maxNotificationsPerSecond(1)
                .maxNumberOfOutstandingNotification(100)
                .apns(
                        RobertPushServerProperties.Apns.builder()
                                .authKeyId("key-id")
                                .topic("topic")
                                .clients(
                                        Collections.singletonList(
                                                RobertPushServerProperties.ApnsClient.builder().host("localhost")
                                                        .port(443).build()
                                        )
                                )
                                .build()
                )
                .build();

        service = new ApnsPushNotificationService(robertPushServerProperties, pushInfoDao, apnsClientFactory);

        PushInfo pushInfo = PushInfo.builder().token("123456789").build();

        // when
        long currentTimeBeforeSendingNotif = System.currentTimeMillis();
        IntStream.range(0, 5).forEach(i -> service.sendPushNotification(pushInfo));
        long currentTimeAfterSendingNotif = System.currentTimeMillis();

        long durationSendingNotif = currentTimeAfterSendingNotif - currentTimeBeforeSendingNotif;

        // then
        assertThat(durationSendingNotif).isGreaterThan(3000);

    }

    @Test
    public void shouldSendNotificationFastly() {

        // given
        RobertPushServerProperties robertPushServerProperties = RobertPushServerProperties
                .builder()
                .maxNotificationsPerSecond(100)
                .maxNumberOfOutstandingNotification(100)
                .apns(
                        RobertPushServerProperties.Apns.builder()
                                .authKeyId("key-id")
                                .topic("topic")
                                .clients(
                                        Collections.singletonList(
                                                RobertPushServerProperties.ApnsClient.builder().host("localhost")
                                                        .port(443).build()
                                        )
                                )
                                .build()
                )
                .build();

        service = new ApnsPushNotificationService(robertPushServerProperties, pushInfoDao, apnsClientFactory);

        PushInfo pushInfo = PushInfo.builder().token("123456789").build();

        // when
        long currentTimeBeforeSendingNotif = System.currentTimeMillis();
        IntStream.range(0, 100).forEach(i -> service.sendPushNotification(pushInfo));
        long currentTimeAfterSendingNotif = System.currentTimeMillis();

        long durationSendingNotif = currentTimeAfterSendingNotif - currentTimeBeforeSendingNotif;

        // then
        assertThat(durationSendingNotif).isLessThan(1000);

    }

    class AcceptedPushNotificationResponse implements PushNotificationResponse {

        @Override
        public ApnsPushNotification getPushNotification() {
            return null;
        }

        @Override
        public boolean isAccepted() {
            return true;
        }

        @Override
        public UUID getApnsId() {
            return null;
        }

        @Override
        public String getRejectionReason() {
            return null;
        }

        @Override
        public Optional<Instant> getTokenInvalidationTimestamp() {
            return Optional.empty();
        }
    }

}
