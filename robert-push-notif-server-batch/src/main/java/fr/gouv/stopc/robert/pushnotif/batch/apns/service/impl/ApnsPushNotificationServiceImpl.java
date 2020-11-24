package fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl;

import com.eatthepath.pushy.apns.*;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class ApnsPushNotificationServiceImpl implements IApnsPushNotificationService {

    private final PropertyLoader propertyLoader;
    private ApnsClient apnsClient;
    private ApnsClient secondaryApnsClient;

    @Inject
    public ApnsPushNotificationServiceImpl(PropertyLoader propertyLoader) {
        this.propertyLoader = propertyLoader;
    }

    @PostConstruct
    public void initApnsClient() throws InvalidKeyException, NoSuchAlgorithmException, IOException {

        String secondaryApnsHost = ApnsClientBuilder.PRODUCTION_APNS_HOST;

        log.info("Configured default anps host as {}", this.propertyLoader.getApnsHost().equals(ApnsClientBuilder.PRODUCTION_APNS_HOST) ?
                "production" : "developement");
        this.apnsClient = new ApnsClientBuilder()
                .setApnsServer(this.propertyLoader.getApnsHost())
                .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(this.propertyLoader.getApnsAuthTokenFile()),
                        this.propertyLoader.getApnsTeamId(),
                        this.propertyLoader.getApnsAuthKeyId()))
                .build();

        if (this.propertyLoader.isEnableSecondaryPush()) {

            if (this.propertyLoader.getApnsHost().equals(ApnsClientBuilder.PRODUCTION_APNS_HOST)) {
                secondaryApnsHost = ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
                log.info("Configured secondary anps host as developement");
            }

            this.secondaryApnsClient = new ApnsClientBuilder()
                    .setApnsServer(secondaryApnsHost)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(this.propertyLoader.getApnsAuthTokenFile()),
                            this.propertyLoader.getApnsTeamId(),
                            this.propertyLoader.getApnsAuthKeyId()))
                    .build();
        }

    }

    private SimpleApnsPushNotification buildPushNotification(PushInfo push) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setContentAvailable(true);
        payloadBuilder.setBadgeNumber(0);

        final String payload = payloadBuilder.build();
        final String token = TokenUtil.sanitizeTokenString(push.getToken());

        return new SimpleApnsPushNotification(token.toLowerCase(), this.propertyLoader.getApnsTopic(), payload,
                Instant.now().plus(SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD), DeliveryPriority.IMMEDIATE, PushType.BACKGROUND);

    }

    @Override
    public PushInfo sendPushNotification(PushInfo push) {

        if (Objects.isNull(push)) {
            return push;
        }

        return this.sendNotification(push, this.propertyLoader.isEnableSecondaryPush());
    }

    private PushInfo sendNotification(PushInfo push, boolean useSecondaryApns) {


        final SimpleApnsPushNotification pushNotification = buildPushNotification(push);
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;

        if (useSecondaryApns) {

            sendNotificationFuture = this.secondaryApnsClient.sendNotification(pushNotification);
        } else {
            sendNotificationFuture = this.apnsClient.sendNotification(pushNotification);

        }

        sendNotificationFuture.whenComplete((response, cause) -> {
            if (Objects.isNull(response)) {
                // Handle the push notification response as before from here.
                log.debug("Push Notification successful sent => {}", response);
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                log.info("Push Notification failed => {}", cause.getMessage());
            }
        });

        try {

            log.debug("Push notification accepted by APNs gateway for the token ({})", push.getToken());

            push.setActive(true);
            push.setLastSuccessfulPush(TimeUtils.getNowAtTimeZoneUTC());
            push.setSuccessfulPushSent(push.getSuccessfulPushSent() + 1);

        } catch (Exception e) {
            log.error("Failed to send push notification due to {}.", e.getMessage());

            push.setLastFailurePush(TimeUtils.getNowAtTimeZoneUTC());
            push.setFailedPushSent(push.getFailedPushSent() + 1);

        } finally {
            this.setNextPlannedPushDate(push);
        }
        return push;

    }

    private void setNextPlannedPushDate(PushInfo push) {
        PushDate pushDate = PushDate.builder()
                .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                .timezone(push.getTimezone())
                .minPushHour(this.propertyLoader.getMinPushHour())
                .maxPushHour(this.propertyLoader.getMaxPushHour())
                .build();

        TimeUtils.getNextPushDate(pushDate).ifPresent(push::setNextPlannedPush);

    }

    @Override
    public void close() {

        if (Objects.nonNull(this.apnsClient)) {
            CompletableFuture<Void> close = this.apnsClient.close();
            close.whenComplete((response, cause) -> {

                if (Objects.nonNull(this.secondaryApnsClient)) {
                    this.secondaryApnsClient.close();
                }
            });

        }
    }

}
