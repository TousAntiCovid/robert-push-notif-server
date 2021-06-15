package fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.net.ssl.SSLException;

import org.springframework.stereotype.Service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.DeliveryPriority;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.PushType;
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
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ApnsPushNotificationServiceImpl implements IApnsPushNotificationService {

    private final PropertyLoader propertyLoader;
    private ApnsClient apnsClient;
    private ApnsClient secondaryApnsClient;
    private IPushInfoService pushInfoService;

    @Inject
    public ApnsPushNotificationServiceImpl(PropertyLoader propertyLoader, IPushInfoService pushInfoService) {
        this.propertyLoader = propertyLoader;
        this.pushInfoService = pushInfoService;
    }

    @PostConstruct
    public void initApnsClient() throws InvalidKeyException, SSLException, NoSuchAlgorithmException, IOException {
        String secondaryApnsHost = ApnsClientBuilder.PRODUCTION_APNS_HOST;

        log.debug("Configured default anps host as {}", this.propertyLoader.getApnsHost().equals(ApnsClientBuilder.PRODUCTION_APNS_HOST) ?
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
                log.debug("Configured secondary anps host as developement");
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
            return null ;
        }

        return this.sendNotification(push, this.propertyLoader.isEnableSecondaryPush());
    }

    private PushInfo sendNotification(PushInfo push, boolean useSecondaryApns) {

        CompletableFuture.runAsync(() -> {
            final SimpleApnsPushNotification pushNotification = buildPushNotification(push);
            final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;

            try {
                if (useSecondaryApns) {

                    sendNotificationFuture = this.secondaryApnsClient.sendNotification(pushNotification);
                } else {
                    sendNotificationFuture = this.apnsClient.sendNotification(pushNotification);

                }
                final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
                        sendNotificationFuture.get();

                if (pushNotificationResponse.isAccepted()) {
                    log.debug("Push notification accepted by APNs gateway for the token ({})", push.getToken());
                    push.setActive(true);
                    push.setLastSuccessfulPush(TimeUtils.getNowAtTimeZoneUTC());
                    push.setSuccessfulPushSent(push.getSuccessfulPushSent() + 1);
                    this.setNextPlannedPushDate(push);

                } else {
                    log.debug("Notification rejected by the APNs gateway: {}",
                            pushNotificationResponse.getRejectionReason());
                    final String rejetctionReason = pushNotificationResponse.getRejectionReason();

                    if(StringUtils.isNotBlank(rejetctionReason) && this.propertyLoader.getApnsInactiveRejectionReason().contains(rejetctionReason)) {

                        if (useSecondaryApns) {
                            this.sendNotification(push, false);
                        } else {
                            push.setActive(false);
                        }
                    }

                    if(StringUtils.isNotBlank(rejetctionReason) && !useSecondaryApns) {
                        push.setLastErrorCode(rejetctionReason);
                        push.setLastFailurePush(TimeUtils.getNowAtTimeZoneUTC());
                        push.setFailedPushSent(push.getFailedPushSent() + 1);

                    }

                    pushNotificationResponse.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
                        log.debug("\t…and the token is invalid as of {}", timestamp);
                    });

                }
                this.pushInfoService.saveAll(Arrays.asList(push));
                sendNotificationFuture.whenComplete((response, cause) -> {
                    if (Objects.nonNull(response)) {
                        // Handle the push notification response as before from here.
                        log.debug("Push Notification successful sent => {}", response);
                    } else {
                        // Something went wrong when trying to send the notification to the
                        // APNs server. Note that this is distinct from a rejection from
                        // the server, and indicates that something went wrong when actually
                        // sending the notification or waiting for a reply.
                        log.debug("Push Notification failed => {}", cause);
                    }

                });


            } catch (final ExecutionException | InterruptedException e) {
                log.error("Failed to send push notification due to {}.", e.getMessage());

                push.setLastFailurePush(TimeUtils.getNowAtTimeZoneUTC());
                push.setFailedPushSent(push.getFailedPushSent() + 1);
                push.setLastErrorCode(e.getMessage());
            } finally {
                this.setNextPlannedPushDate(push);
            }


        });

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
            log.info("Beginning of the close of the main apns client");
            CompletableFuture<Void> close = this.apnsClient.close();
            close.whenComplete((response, cause) -> {
                log.info("Close of the main apnsClient has been successfully completed");
                if (Objects.nonNull(this.secondaryApnsClient)) {
                    log.info("Beginning of the close of the secondary apns client");
                    CompletableFuture<Void> closeSecondary = this.secondaryApnsClient.close();
                    closeSecondary.whenComplete((response2, cause2) -> {
                        log.info("Close of the secondary apns client has been successfully completed");
                    });
                }
            });
        }
    }

}
