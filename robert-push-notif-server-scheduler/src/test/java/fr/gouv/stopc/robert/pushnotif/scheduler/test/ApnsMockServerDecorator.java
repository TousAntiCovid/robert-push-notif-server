package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.server.*;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.SSLSession;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

@ToString
class ApnsMockServerDecorator extends ParsingMockApnsServerListenerAdapter {

    private final int port;

    private MockApnsServer mock = initMock(emptyMap());

    @Getter
    private final Queue<ApnsPushNotification> acceptedPushNotifications = new ConcurrentLinkedQueue<>();

    @Getter
    private final Queue<ApnsPushNotification> rejectedPushNotifications = new ConcurrentLinkedQueue<>();

    ApnsMockServerDecorator(final int port) {
        this.port = port;
        try {
            mock.start(port).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private MockApnsServer initMock(final Map<String, RejectionReason> rejectionReasonPerTokenMap) {
        return new MockApnsServerBuilder()
                .setServerCredentials(
                        new ClassPathResource("/apns/server-certs.pem").getInputStream(),
                        new ClassPathResource("/apns/server-key.pem").getInputStream(), null
                )
                .setTrustedClientCertificateChain(new ClassPathResource("/apns/ca.pem").getInputStream())
                .setEventLoopGroup(new NioEventLoopGroup(2))
                .setListener(this)
                .setHandlerFactory(new CustomValidationPushNotificationHandlerFactory(rejectionReasonPerTokenMap))
                .build();
    }

    void clear() {
        acceptedPushNotifications.clear();
        rejectedPushNotifications.clear();
    }

    void resetMockWithRejectedToken(final String token, final RejectionReason rejectionReason) {
        try {
            mock.shutdown()
                    .thenAccept(ignored -> mock = initMock(Map.of(token, rejectionReason)))
                    .thenAccept(ignored -> mock.start(port))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    void resetMock() {
        try {
            mock.shutdown()
                    .thenAccept(ignored -> mock = initMock(emptyMap()))
                    .thenAccept(ignored -> mock.start(port))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handlePushNotificationAccepted(final ApnsPushNotification pushNotification) {
        acceptedPushNotifications.add(pushNotification);
    }

    @Override
    public void handlePushNotificationRejected(final ApnsPushNotification pushNotification,
            final com.eatthepath.pushy.apns.server.RejectionReason rejectionReason,
            final Instant deviceTokenExpirationTimestamp) {
        rejectedPushNotifications.add(pushNotification);
    }

    private static class CustomValidationPushNotificationHandlerFactory implements PushNotificationHandlerFactory {

        private final Map<String, com.eatthepath.pushy.apns.server.RejectionReason> rejectionReasonPerTokenMap;

        public CustomValidationPushNotificationHandlerFactory(final Map<String, RejectionReason> map) {
            this.rejectionReasonPerTokenMap = map.entrySet().stream().collect(
                    Collectors.toMap(
                            Map.Entry::getKey,
                            value -> com.eatthepath.pushy.apns.server.RejectionReason.valueOf(value.getValue().name())
                    )
            );
        }

        @Override
        public PushNotificationHandler buildHandler(final SSLSession sslSession) {
            return new CustomValidationPushNotificationHandler(rejectionReasonPerTokenMap);
        }
    }

    /**
     * This custom {@link PushNotificationHandler} validates the received
     * notification and rejects some of them depending on the input map :
     * <ul>
     * <li>reason = BAD_DEVICE_TOKEN in case device's token is equal to
     * 987654321</li>
     * </ul>
     */
    @RequiredArgsConstructor
    private static class CustomValidationPushNotificationHandler implements PushNotificationHandler {

        private static final String APNS_PATH_PREFIX = "/3/device/";

        private final Map<String, com.eatthepath.pushy.apns.server.RejectionReason> rejectionReasonPerTokenMap;

        @Override
        public void handlePushNotification(final Http2Headers headers, final ByteBuf payload)
                throws RejectedNotificationException {
            final CharSequence pathSequence = headers.get(Http2Headers.PseudoHeaderName.PATH.value());

            if (pathSequence != null) {
                final String pathString = pathSequence.toString();
                final String deviceToken = pathString.substring(APNS_PATH_PREFIX.length());

                if (rejectionReasonPerTokenMap.containsKey(deviceToken)) {
                    throw new RejectedNotificationException(rejectionReasonPerTokenMap.get(deviceToken));
                }
            }
        }
    }
}
