package test.fr.gouv.stopc.robert.pushnotif.batch.it;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.server.MockApnsServer;
import com.eatthepath.pushy.apns.server.MockApnsServerBuilder;
import com.eatthepath.pushy.apns.server.MockApnsServerListener;
import com.eatthepath.pushy.apns.server.ParsingMockApnsServerListenerAdapter;
import com.eatthepath.pushy.apns.server.PushNotificationHandler;
import com.eatthepath.pushy.apns.server.PushNotificationHandlerFactory;
import com.eatthepath.pushy.apns.server.RejectedNotificationException;
import com.eatthepath.pushy.apns.server.RejectionReason;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import javax.net.ssl.SSLSession;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

/**
 * A {@link TestExecutionListener} to start APNs server mocks to be used as a
 * dependency for SpringBootTests.
 * <p>
 * It starts a Apns servers export required system properties to override Spring
 * application context configuration.
 * <p>
 * Static methods
 * {@link APNsServersManager#awaitMainAcceptedQueueContainsAtLeast(int)},
 * {@link APNsServersManager#awaitMainRejectedQueueContainsAtLeast(int)},
 * {@link APNsServersManager#awaitSecondaryAcceptedQueueContainsAtLeast(int)},
 * and
 * {@link APNsServersManager#awaitSecondaryRejectedQueueContainsAtLeast(int)}
 * can be used to fetch notifications received by Apns Server.
 */
@Slf4j
public class APNsServersManager implements TestExecutionListener {

    private static Queue<ApnsPushNotification> MAIN_ACCEPTED_PUSH_NOTIFICATIONS = new ConcurrentLinkedQueue<>();

    private static Queue<ApnsPushNotification> MAIN_REJECTED_NOTIFICATIONS = new ConcurrentLinkedQueue<>();

    private static Queue<ApnsPushNotification> SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS = new ConcurrentLinkedQueue<>();

    private static Queue<ApnsPushNotification> SECONDARY_REJECTED_NOTIFICATIONS = new ConcurrentLinkedQueue<>();

    private static MockApnsServer mainApnsServer;

    private static MockApnsServer secondaryApnsServer;

    protected static NioEventLoopGroup MAIN_SERVER_EVENT_LOOP_GROUP;

    protected static NioEventLoopGroup SECONDARY_SERVER_EVENT_LOOP_GROUP;

    protected static final Resource CA_CERTIFICATE_FILENAME = new ClassPathResource("/ca.pem");

    protected static final Resource SERVER_CERTIFICATES_FILENAME = new ClassPathResource("/server-certs.pem");

    protected static final Resource SERVER_KEY_FILENAME = new ClassPathResource("/server-key.pem");

    public static final int MAIN_SERVER_PORT = 443;

    public static final int SECONDARY_SERVER_PORT = 2197;

    static {
        MAIN_SERVER_EVENT_LOOP_GROUP = new NioEventLoopGroup(2);
        SECONDARY_SERVER_EVENT_LOOP_GROUP = new NioEventLoopGroup(2);
        System.setProperty("robert.push.notif.server.apns.main-server-port", String.valueOf(MAIN_SERVER_PORT));
        System.setProperty(
                "robert.push.notif.server.apns.secondary-server-port", String.valueOf(SECONDARY_SERVER_PORT)
        );
    }

    @SneakyThrows
    public APNsServersManager() {

        mainApnsServer = buildMockApnsServer(
                MAIN_SERVER_EVENT_LOOP_GROUP, new TestMockApnsServerListener(ApnServerType.MAIN)
        );
        mainApnsServer.start(MAIN_SERVER_PORT);

        secondaryApnsServer = buildMockApnsServer(
                SECONDARY_SERVER_EVENT_LOOP_GROUP, new TestMockApnsServerListener(ApnServerType.SECONDARY)
        );
        secondaryApnsServer.start(SECONDARY_SERVER_PORT);
    }

    @SneakyThrows
    private MockApnsServer buildMockApnsServer(NioEventLoopGroup nioEventLoopGroup,
            MockApnsServerListener listener) {
        return new MockApnsServerBuilder()
                .setServerCredentials(
                        SERVER_CERTIFICATES_FILENAME.getInputStream(),
                        SERVER_KEY_FILENAME.getInputStream(), null
                )
                .setTrustedClientCertificateChain(CA_CERTIFICATE_FILENAME.getInputStream())
                .setEventLoopGroup(nioEventLoopGroup)
                .setHandlerFactory(new CustomValidationPushNotificationHandlerFactory())
                .setListener(listener)
                .build();
    }

    @Override
    public void beforeTestExecution(TestContext testContext) throws Exception {
        TestExecutionListener.super.beforeTestExecution(testContext);
        cleanQueues();
    }

    public static void cleanQueues() {
        MAIN_ACCEPTED_PUSH_NOTIFICATIONS.clear();
        MAIN_REJECTED_NOTIFICATIONS.clear();
        SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS.clear();
        SECONDARY_REJECTED_NOTIFICATIONS.clear();
    }

    public static List<ApnsPushNotification> awaitMainAcceptedQueueContainsAtLeast(int count) {
        return awaitAcceptedQueueContainsAtLeast(MAIN_ACCEPTED_PUSH_NOTIFICATIONS, count);
    }

    public static List<ApnsPushNotification> awaitSecondaryAcceptedQueueContainsAtLeast(int count) {
        return awaitAcceptedQueueContainsAtLeast(SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS, count);
    }

    private static List<ApnsPushNotification> awaitAcceptedQueueContainsAtLeast(Queue<ApnsPushNotification> queue,
            int count) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> queue.size() >= count);

        return queue.stream().collect(Collectors.toList());
    }

    public static List<ApnsPushNotification> awaitMainRejectedQueueContainsAtLeast(int count) {
        return awaitRejectedQueueContainsAtLeast(MAIN_REJECTED_NOTIFICATIONS, count);
    }

    public static List<ApnsPushNotification> awaitSecondaryRejectedQueueContainsAtLeast(int count) {
        return awaitRejectedQueueContainsAtLeast(SECONDARY_REJECTED_NOTIFICATIONS, count);
    }

    private static List<ApnsPushNotification> awaitRejectedQueueContainsAtLeast(Queue<ApnsPushNotification> queue,
            int count) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> queue.size() >= count);

        return queue.stream().collect(Collectors.toList());
    }

    private enum ApnServerType {
        MAIN,
        SECONDARY
    }

    private class CustomValidationPushNotificationHandlerFactory implements PushNotificationHandlerFactory {

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
            return new CustomValidationPushNotificationHandler();
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
    private class CustomValidationPushNotificationHandler implements PushNotificationHandler {

        private static final String APNS_PATH_PREFIX = "/3/device/";

        @Override
        public void handlePushNotification(Http2Headers headers, ByteBuf payload) throws RejectedNotificationException {
            final CharSequence pathSequence = headers.get(Http2Headers.PseudoHeaderName.PATH.value());

            if (pathSequence != null) {
                final String pathString = pathSequence.toString();

                if (pathSequence.toString().equals(APNS_PATH_PREFIX)) {
                    throw new RejectedNotificationException(RejectionReason.MISSING_DEVICE_TOKEN);
                } else if (pathString.startsWith(APNS_PATH_PREFIX)) {
                    final String deviceToken = pathString.substring(APNS_PATH_PREFIX.length());

                    if (deviceToken.contains("987654321")) {
                        throw new RejectedNotificationException(RejectionReason.BAD_DEVICE_TOKEN);
                    }
                }
            }
        }
    }

    /**
     * This listener stores received and rejected notification to static queues from
     * APNs server point of view
     */
    private class TestMockApnsServerListener extends ParsingMockApnsServerListenerAdapter {

        private ApnServerType apnServerType;

        public TestMockApnsServerListener(ApnServerType apnServerType) {
            this.apnServerType = apnServerType;
        }

        @Override
        public void handlePushNotificationAccepted(final ApnsPushNotification pushNotification) {
            switch (apnServerType) {
                case MAIN:
                    MAIN_ACCEPTED_PUSH_NOTIFICATIONS.add(pushNotification);
                    break;
                case SECONDARY:
                    SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS.add(pushNotification);
                    break;
                default:
                    throw new UnsupportedOperationException("Apn server type [" + apnServerType + "]is not managed !");
            }
        }

        @Override
        public void handlePushNotificationRejected(final ApnsPushNotification pushNotification,
                final RejectionReason rejectionReason, final Instant deviceTokenExpirationTimestamp) {
            switch (apnServerType) {
                case MAIN:
                    MAIN_REJECTED_NOTIFICATIONS.add(pushNotification);
                    break;
                case SECONDARY:
                    SECONDARY_REJECTED_NOTIFICATIONS.add(pushNotification);
                    break;
                default:
                    throw new UnsupportedOperationException("Apn server type [" + apnServerType + "]is not managed !");
            }
        }

    }

}
