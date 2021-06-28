package fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl;

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
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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

    public void initApnsClient() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        String secondaryApnsHost = this.propertyLoader.getApnsDevelopmentHost();

        log.debug(
                "Configured default anps host as {}",
                this.propertyLoader.getApnsHost().equals(ApnsClientBuilder.PRODUCTION_APNS_HOST) ? "production"
                        : "development"
        );
        ApnsClientBuilder apnsClientBuilder = new ApnsClientBuilder()
                .setApnsServer(this.propertyLoader.getApnsHost(), propertyLoader.getApnsMainServerPort())
                .setSigningKey(
                        ApnsSigningKey.loadFromInputStream(
                                this.propertyLoader.getApnsAuthTokenFile().getInputStream(),
                                this.propertyLoader.getApnsTeamId(),
                                this.propertyLoader.getApnsAuthKeyId()
                        )
                );

        if (propertyLoader.getApnsTrustedClientCertificateChain() != null) {
            apnsClientBuilder.setTrustedServerCertificateChain(
                    propertyLoader.getApnsTrustedClientCertificateChain().getInputStream()
            );
        }
        this.apnsClient = apnsClientBuilder.build();

        if (this.propertyLoader.isApnsSecondaryEnable()) {

            ApnsClientBuilder secondaryApnsClientBuilder = new ApnsClientBuilder()
                    .setApnsServer(secondaryApnsHost, propertyLoader.getApnsSecondaryServerPort())
                    .setSigningKey(
                            ApnsSigningKey.loadFromInputStream(
                                    this.propertyLoader.getApnsAuthTokenFile().getInputStream(),
                                    this.propertyLoader.getApnsTeamId(),
                                    this.propertyLoader.getApnsAuthKeyId()
                            )
                    );

            if (propertyLoader.getApnsTrustedClientCertificateChain() != null) {
                secondaryApnsClientBuilder.setTrustedServerCertificateChain(
                        propertyLoader.getApnsTrustedClientCertificateChain().getInputStream()
                );
            }
            this.secondaryApnsClient = secondaryApnsClientBuilder.build();
        }

    }

    private SimpleApnsPushNotification buildPushNotification(PushInfo push) {

        final ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setContentAvailable(true);
        payloadBuilder.setBadgeNumber(0);

        final String payload = payloadBuilder.build();
        final String token = TokenUtil.sanitizeTokenString(push.getToken());

        return new SimpleApnsPushNotification(
                token.toLowerCase(), this.propertyLoader.getApnsTopic(), payload,
                Instant.now().plus(SimpleApnsPushNotification.DEFAULT_EXPIRATION_PERIOD), DeliveryPriority.IMMEDIATE,
                PushType.BACKGROUND
        );

    }

    @Override
    public PushInfo sendPushNotification(PushInfo push) {

        if (Objects.isNull(push)) {
            return null;
        }

        return this.sendNotification(push, this.propertyLoader.isApnsSecondaryEnable());
    }

    private PushInfo sendNotification(PushInfo push, boolean useSecondaryApns) {

        final SimpleApnsPushNotification pushNotification = buildPushNotification(push);
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;

        try {
            if (useSecondaryApns) {

                sendNotificationFuture = this.secondaryApnsClient.sendNotification(pushNotification);
            } else {
                sendNotificationFuture = this.apnsClient.sendNotification(pushNotification);

            }
            final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = sendNotificationFuture
                    .get();

            if (pushNotificationResponse.isAccepted()) {
                log.debug("Push notification accepted by APNs gateway for the token ({})", push.getToken());
                push.setActive(true);
                push.setLastSuccessfulPush(TimeUtils.getNowAtTimeZoneUTC());
                push.setSuccessfulPushSent(push.getSuccessfulPushSent() + 1);
            } else {
                log.debug(
                        "Notification rejected by the APNs gateway: {}",
                        pushNotificationResponse.getRejectionReason()
                );
                final String rejectionReason = pushNotificationResponse.getRejectionReason();

                if (StringUtils.isNotBlank(rejectionReason)
                        && this.propertyLoader.getApnsInactiveRejectionReason().contains(rejectionReason)) {

                    if (useSecondaryApns) {
                        this.sendNotification(push, false);
                    } else {
                        push.setActive(false);
                    }
                }

                if (StringUtils.isNotBlank(rejectionReason) && !useSecondaryApns) {
                    push.setLastErrorCode(rejectionReason);
                    push.setLastFailurePush(TimeUtils.getNowAtTimeZoneUTC());
                    push.setFailedPushSent(push.getFailedPushSent() + 1);

                }

                pushNotificationResponse.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
                    log.debug("\t…and the token is invalid as of {}", timestamp);
                });

            }
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
    public void close() throws ExecutionException, InterruptedException {

        if (Objects.nonNull(this.apnsClient)) {
            CompletableFuture<Void> close = this.apnsClient.close();
            close.get();
            log.info("Closure of the main apnsClient has been successfully completed");
        }
        if (Objects.nonNull(this.secondaryApnsClient)) {
            CompletableFuture<Void> closeSecondary = this.secondaryApnsClient.close();
            closeSecondary.get();
            log.info("Closure of the secondary apnsClient has been successfully completed");
        }
    }
}
