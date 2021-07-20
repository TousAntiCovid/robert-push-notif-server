package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

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
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApnsPushNotificationService {

    private final PropertyLoader propertyLoader;

    private ApnsClient apnsClient;

    private ApnsClient secondaryApnsClient;

    private final PushInfoDao pushInfoDao;

    @PostConstruct
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

    public void sendPushNotification(PushInfo push) {
        this.sendNotification(push, this.propertyLoader.isApnsSecondaryEnable());
    }

    private void sendNotification(PushInfo push, boolean useSecondaryApns) {
        final SimpleApnsPushNotification pushNotification = buildPushNotification(push);
        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture;

        if (useSecondaryApns) {

            sendNotificationFuture = this.secondaryApnsClient.sendNotification(pushNotification);
        } else {
            sendNotificationFuture = this.apnsClient.sendNotification(pushNotification);

        }

        sendNotificationFuture.whenComplete((response, cause) -> {
            if (Objects.nonNull(response)) {
                if (response.isAccepted()) {
                    log.debug("Push notification accepted by APNs gateway for the token ({})", push.getToken());
                    push.setLastSuccessfulPush(LocalDateTime.now());
                    push.setSuccessfulPushSent(push.getSuccessfulPushSent() + 1);
                    pushInfoDao.updateSuccessFulPushedNotif(push);
                } else {
                    log.debug(
                            "Notification rejected by the APNs gateway: {}",
                            response.getRejectionReason()
                    );
                    final String rejectionReason = response.getRejectionReason();

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
                        push.setLastFailurePush(LocalDateTime.now());
                        push.setFailedPushSent(push.getFailedPushSent() + 1);
                        pushInfoDao.updateFailurePushedNotif(push);
                    }

                    response.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
                        log.debug("\t…and the token is invalid as of {}", timestamp);
                    });
                }
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs server. Note that this is distinct from a rejection from
                // the server, and indicates that something went wrong when actually
                // sending the notification or waiting for a reply.
                log.warn("Push Notification failed => {}", cause);
                // mettre à jour le lastErrorCode et lastFailurePushDate?
            }

        }).exceptionally(e -> {
            // TODO voir pour une meilleure façon de gérer les exceptions !
            log.error("Unexpected error occurred !!", e);
            return null;
        });
    }
}
