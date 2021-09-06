package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushType;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.ApnsClientFactory;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import io.github.bucket4j.*;
import io.github.bucket4j.local.LockFreeBucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class ApnsPushNotificationService {

    private final RobertPushServerProperties robertPushServerProperties;

    private final PushInfoDao pushInfoDao;

    private final ApnsClientFactory apnsClientFactory;

    private final Semaphore semaphore;

    private final BlockingBucket rateLimitingBucket;

    public ApnsPushNotificationService(
            RobertPushServerProperties robertPushServerProperties, PushInfoDao pushInfoDao,
            ApnsClientFactory apnsClientFactory) {
        this.robertPushServerProperties = robertPushServerProperties;
        this.pushInfoDao = pushInfoDao;
        this.apnsClientFactory = apnsClientFactory;

        semaphore = new Semaphore(robertPushServerProperties.getMaxNumberOfOutstandingNotification());

        Bandwidth limit = Bandwidth.classic(
                robertPushServerProperties.getMaxNotificationsPerSecond(),
                Refill.intervally(
                        robertPushServerProperties.getMaxNotificationsPerSecond(),
                        Duration.ofSeconds(1)
                )
        );
        BucketConfiguration configuration = Bucket4j.configurationBuilder().addLimit(limit).build();
        this.rateLimitingBucket = new LockFreeBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public void waitUntilNoActivity(Duration tolerance) {
        return this.semaphore.availablePermits();
    }

    private SimpleApnsPushNotification buildPushNotification(PushInfo push) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setContentAvailable(true);
        payloadBuilder.setBadgeNumber(0);

        final String payload = payloadBuilder.build();
        final String token = TokenUtil.sanitizeTokenString(push.getToken());

        return new SimpleApnsPushNotification(
                token.toLowerCase(), this.robertPushServerProperties.getApns().getTopic(), payload,
                Instant.now().plus(SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD), DeliveryPriority.IMMEDIATE,
                PushType.BACKGROUND
        );

    }

    public void sendPushNotification(PushInfo push) {
        this.sendNotification(push, new ConcurrentLinkedQueue<>(apnsClientFactory.getApnsClients()));
    }

    private sendNotification(PushInfo push, Queue<RateLimitedApnsClient> apnsClientsQueue) {
        final SimpleApnsPushNotification pushNotification = buildPushNotification(push);
        RateLimitedApnsClient apnsClient = apnsClientsQueue.poll();
      return  apnsClient.sendNotification(pushNotification)
                .onComplete(
                        success -> {
                            log.debug(
                                    "Push notification sent by {} accepted by APNs gateway for the token ({})",
                                    apnsClient.getId(), push.getToken()
                            );
                            push.setLastSuccessfulPush(LocalDateTime.now());
                            push.setSuccessfulPushSent(push.getSuccessfulPushSent() + 1);
                            pushInfoDao.updateSuccessFulPushedNotif(push);
                        },
                        error -> {
                            if (error == RateLimitedApnsClient.RejectedReasonCategory.INACTIVE) {
                                if (apnsClientsQueue.isEmpty()) {
                                    push.setActive(false);
                                    push.setLastErrorCode(rejectionReason);
                                    push.setLastFailurePush(LocalDateTime.now());
                                    push.setFailedPushSent(push.getFailedPushSent() + 1);
                                    pushInfoDao.updateFailurePushedNotif(push);
                                } else {
                                    this.sendNotification(push, apnsClientsQueue);
                                }
                            } else {
                                push.setLastErrorCode(rejectionReason);
                                push.setLastFailurePush(LocalDateTime.now());
                                push.setFailedPushSent(push.getFailedPushSent() + 1);
                                pushInfoDao.updateFailurePushedNotif(push);
                            }
                        }
                );
    }
}
