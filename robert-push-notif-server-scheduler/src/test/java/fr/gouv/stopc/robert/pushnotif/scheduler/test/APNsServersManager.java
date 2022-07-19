package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.server.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import javax.net.ssl.SSLSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.eatthepath.pushy.apns.server.RejectionReason.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link TestExecutionListener} to start APNs server mocks to be used as a
 * dependency for SpringBootTests.
 */
public class APNsServersManager implements TestExecutionListener {

    private static final APNsServerExecutionContext MAIN_APNS_SERVER_EXEC_CONTEXT = new APNsServerExecutionContext();

    private static final APNsServerExecutionContext SECONDARY_APNS_SERVER_EXEC_CONTEXT = new APNsServerExecutionContext();

    static {
        MockApnsServer mainApnsServer = buildMockApnsServer(
                MAIN_APNS_SERVER_EXEC_CONTEXT,
                Map.of(
                        "987654321", BAD_DEVICE_TOKEN,
                        "123456789", BAD_DEVICE_TOKEN,
                        "8888888888", BAD_DEVICE_TOKEN,
                        "999999999", BAD_TOPIC,
                        "112233445566", BAD_MESSAGE_ID
                )
        );
        mainApnsServer.start(2198);

        MockApnsServer secondaryApnsServer = buildMockApnsServer(
                SECONDARY_APNS_SERVER_EXEC_CONTEXT,
                Map.of(
                        "987654321", BAD_DEVICE_TOKEN,
                        "8888888888", PAYLOAD_EMPTY
                )
        );
        secondaryApnsServer.start(2197);
    }

    @Override
    public void beforeTestExecution(final TestContext testContext) throws Exception {
        TestExecutionListener.super.beforeTestExecution(testContext);
        MAIN_APNS_SERVER_EXEC_CONTEXT.clear();
        SECONDARY_APNS_SERVER_EXEC_CONTEXT.clear();
    }

    public static List<ApnsPushNotification> getNotifsAcceptedBySecondServer() {
        return new ArrayList<>(SECONDARY_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications());
    }

    public static List<ApnsPushNotification> getNotifsAcceptedByMainServer() {
        return new ArrayList<>(MAIN_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications());
    }

    public static void assertThatMainServerAcceptedOne() {
        assertThat(MAIN_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications()).hasSize(1);
    }

    public static void assertThatMainServerAccepted(final int nbNotifications) {
        assertThat(MAIN_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications()).hasSize(nbNotifications);
    }

    public static void assertThatMainServerAcceptedNothing() {
        assertThat(MAIN_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications()).isEmpty();
    }

    public static void assertThatMainServerRejectedOne() {
        assertThat(MAIN_APNS_SERVER_EXEC_CONTEXT.getRejectedPushNotifications()).hasSize(1);
    }

    public static void assertThatMainServerRejectedNothing() {
        assertThat(MAIN_APNS_SERVER_EXEC_CONTEXT.getRejectedPushNotifications()).isEmpty();
    }

    public static void assertThatSecondServerAcceptedOne() {
        assertThat(SECONDARY_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications()).hasSize(1);
    }

    public static void assertThatSecondServerAcceptedNothing() {
        assertThat(SECONDARY_APNS_SERVER_EXEC_CONTEXT.getAcceptedPushNotifications()).isEmpty();
    }

    public static void assertThatSecondServerRejectedOne() {
        assertThat(SECONDARY_APNS_SERVER_EXEC_CONTEXT.getRejectedPushNotifications()).hasSize(1);
    }

    public static void assertThatSecondServerRejectedNothing() {
        assertThat(SECONDARY_APNS_SERVER_EXEC_CONTEXT.getRejectedPushNotifications()).isEmpty();
    }

    @SneakyThrows
    private static MockApnsServer buildMockApnsServer(final APNsServerExecutionContext apnsServerExecutionContext,
            final Map<String, RejectionReason> rejectionReasonPerTokenMap) {
        return new MockApnsServerBuilder()
                .setServerCredentials(
                        new ClassPathResource("/apns/server-certs.pem").getInputStream(),
                        new ClassPathResource("/apns/server-key.pem").getInputStream(), null
                )
                .setTrustedClientCertificateChain(new ClassPathResource("/apns/ca.pem").getInputStream())
                .setEventLoopGroup(new NioEventLoopGroup(2))
                .setHandlerFactory(new CustomValidationPushNotificationHandlerFactory(rejectionReasonPerTokenMap))
                .setListener(new TestMockApnsServerListener(apnsServerExecutionContext))
                .build();
    }

    @RequiredArgsConstructor
    private static class CustomValidationPushNotificationHandlerFactory implements PushNotificationHandlerFactory {

        private final Map<String, RejectionReason> rejectionReasonPerTokenMap;

        /**
         * Constructs a new push notification handler that unconditionally accepts all
         * push notifications.
         *
         * @param sslSession the SSL session associated with the channel for which this
         *                   handler will handle notifications
         * @return a new "accept everything" push notification handler
         */
        @Override
        public PushNotificationHandler buildHandler(final SSLSession sslSession) {
            return new CustomValidationPushNotificationHandler(rejectionReasonPerTokenMap);
        }
    }

    /**
     * This custom {@link PushNotificationHandler} validates the received
     * notification and rejects some of them depending on some conditions :
     * <ul>
     * <li>reason = BAD_DEVICE_TOKEN in case device's token is equal to
     * 987654321</li>
     * </ul>
     */
    @RequiredArgsConstructor
    private static class CustomValidationPushNotificationHandler implements PushNotificationHandler {

        private static final String APNS_PATH_PREFIX = "/3/device/";

        private final Map<String, RejectionReason> rejectionReasonPerTokenMap;

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

    /**
     * This listener stores received and rejected notification to static queues from
     * APNs server point of view
     */
    private static class TestMockApnsServerListener extends ParsingMockApnsServerListenerAdapter {

        private final APNsServerExecutionContext executionContext;

        public TestMockApnsServerListener(final APNsServerExecutionContext executionContext) {
            this.executionContext = executionContext;
        }

        @Override
        public void handlePushNotificationAccepted(final ApnsPushNotification pushNotification) {
            executionContext.getAcceptedPushNotifications().add(pushNotification);
        }

        @Override
        public void handlePushNotificationRejected(final ApnsPushNotification pushNotification,
                final RejectionReason rejectionReason, final Instant deviceTokenExpirationTimestamp) {
            executionContext.getRejectedPushNotifications().add(pushNotification);
        }

    }

    private static class APNsServerExecutionContext {

        private final Queue<ApnsPushNotification> acceptedPushNotifications = new ConcurrentLinkedQueue<>();

        private final Queue<ApnsPushNotification> rejectedPushNotifications = new ConcurrentLinkedQueue<>();

        public Queue<ApnsPushNotification> getAcceptedPushNotifications() {
            return acceptedPushNotifications;
        }

        public Queue<ApnsPushNotification> getRejectedPushNotifications() {
            return rejectedPushNotifications;
        }

        public void clear() {
            acceptedPushNotifications.clear();
            rejectedPushNotifications.clear();
        }
    }

}
