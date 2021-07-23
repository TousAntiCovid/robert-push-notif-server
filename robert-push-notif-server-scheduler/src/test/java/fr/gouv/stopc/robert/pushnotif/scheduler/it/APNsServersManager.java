package fr.gouv.stopc.robert.pushnotif.scheduler.it;

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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import javax.net.ssl.SSLSession;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * {@link APNsServersManager#awaitMainAcceptedQueueContainsAtLeast(int, Duration)},
 * {@link APNsServersManager#awaitMainRejectedQueueContainsAtLeast(int, Duration)},
 * {@link APNsServersManager#awaitSecondaryAcceptedQueueContainsAtLeast(int, Duration)},
 * and
 * {@link APNsServersManager#awaitSecondaryRejectedQueueContainsAtLeast(int, Duration)}
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

    private static final Map<String, RejectionReason> MAIN_REJECTION_REASON_PER_TOKEN_MAP = new HashMap<>();

    private static final Map<String, RejectionReason> SECONDARY_REJECTION_REASON_PER_TOKEN_MAP = new HashMap<>();

    protected static final Resource CA_CERTIFICATE_FILENAME = new ClassPathResource("/apns/ca.pem");

    protected static final Resource SERVER_CERTIFICATES_FILENAME = new ClassPathResource("/apns/server-certs.pem");

    protected static final Resource SERVER_KEY_FILENAME = new ClassPathResource("/apns/server-key.pem");

    public static final int MAIN_SERVER_PORT = 443;

    public static final int SECONDARY_SERVER_PORT = 2197;

    static {
        MAIN_SERVER_EVENT_LOOP_GROUP = new NioEventLoopGroup(2);
        SECONDARY_SERVER_EVENT_LOOP_GROUP = new NioEventLoopGroup(2);
        MAIN_REJECTION_REASON_PER_TOKEN_MAP.put("987654321", RejectionReason.BAD_DEVICE_TOKEN);
        MAIN_REJECTION_REASON_PER_TOKEN_MAP.put("123456789", RejectionReason.BAD_DEVICE_TOKEN);
        MAIN_REJECTION_REASON_PER_TOKEN_MAP.put("112233445566", RejectionReason.BAD_MESSAGE_ID);

        SECONDARY_REJECTION_REASON_PER_TOKEN_MAP.put("987654321", RejectionReason.BAD_DEVICE_TOKEN);

        System.setProperty("robert.push.server.apns.main-server-port", String.valueOf(MAIN_SERVER_PORT));
        System.setProperty(
                "robert.push.server.apns.secondary-server-port", String.valueOf(SECONDARY_SERVER_PORT)
        );
    }

    @SneakyThrows
    public APNsServersManager() {

        mainApnsServer = buildMockApnsServer(
                MAIN_SERVER_EVENT_LOOP_GROUP,
                new TestMockApnsServerListener(ApnServerType.MAIN),
                MAIN_REJECTION_REASON_PER_TOKEN_MAP
        );
        mainApnsServer.start(MAIN_SERVER_PORT);

        secondaryApnsServer = buildMockApnsServer(
                SECONDARY_SERVER_EVENT_LOOP_GROUP,
                new TestMockApnsServerListener(ApnServerType.SECONDARY),
                SECONDARY_REJECTION_REASON_PER_TOKEN_MAP
        );
        secondaryApnsServer.start(SECONDARY_SERVER_PORT);
    }

    @SneakyThrows
    private MockApnsServer buildMockApnsServer(NioEventLoopGroup nioEventLoopGroup,
            MockApnsServerListener listener, Map<String, RejectionReason> rejectionReasonPerTokenMap) {
        return new MockApnsServerBuilder()
                .setServerCredentials(
                        SERVER_CERTIFICATES_FILENAME.getInputStream(),
                        SERVER_KEY_FILENAME.getInputStream(), null
                )
                .setTrustedClientCertificateChain(CA_CERTIFICATE_FILENAME.getInputStream())
                .setEventLoopGroup(nioEventLoopGroup)
                .setHandlerFactory(new CustomValidationPushNotificationHandlerFactory(rejectionReasonPerTokenMap))
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

    public static List<ApnsPushNotification> awaitMainAcceptedQueueContainsAtLeast(int count, Duration atMostDuration) {
        return awaitAcceptedQueueContainsAtLeast(MAIN_ACCEPTED_PUSH_NOTIFICATIONS, count, atMostDuration);
    }

    public static List<ApnsPushNotification> awaitSecondaryAcceptedQueueContainsAtLeast(int count,
            Duration atMostDuration) {
        return awaitAcceptedQueueContainsAtLeast(SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS, count, atMostDuration);
    }

    private static List<ApnsPushNotification> awaitAcceptedQueueContainsAtLeast(Queue<ApnsPushNotification> queue,
            int count, Duration atMostDuration) {
        await().atMost(atMostDuration)
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> queue.size() >= count);

        return queue.stream().collect(Collectors.toList());
    }

    public static List<ApnsPushNotification> awaitMainRejectedQueueContainsAtLeast(int count, Duration atMostDuration) {
        return awaitRejectedQueueContainsAtLeast(MAIN_REJECTED_NOTIFICATIONS, count, atMostDuration);
    }

    public static List<ApnsPushNotification> awaitSecondaryRejectedQueueContainsAtLeast(int count,
            Duration atMostDuration) {
        return awaitRejectedQueueContainsAtLeast(SECONDARY_REJECTED_NOTIFICATIONS, count, atMostDuration);
    }

    private static List<ApnsPushNotification> awaitRejectedQueueContainsAtLeast(Queue<ApnsPushNotification> queue,
            int count, Duration atMostDuration) {
        await().atMost(atMostDuration)
                .pollInterval(Duration.ofSeconds(1))
                .until(() -> queue.size() >= count);

        return queue.stream().collect(Collectors.toList());
    }

    private enum ApnServerType {
        MAIN,
        SECONDARY
    }

    @RequiredArgsConstructor
    private class CustomValidationPushNotificationHandlerFactory implements PushNotificationHandlerFactory {

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
    private class CustomValidationPushNotificationHandler implements PushNotificationHandler {

        private static final String APNS_PATH_PREFIX = "/3/device/";

        private final Map<String, RejectionReason> rejectionReasonPerTokenMap;

        @Override
        public void handlePushNotification(Http2Headers headers, ByteBuf payload) throws RejectedNotificationException {
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
                    log.info("main accepted push notifications : {}", MAIN_ACCEPTED_PUSH_NOTIFICATIONS.size());
                    break;
                case SECONDARY:
                    SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS.add(pushNotification);
                    log.info(
                            "secondary accepted push notifications : {}", SECONDARY_ACCEPTED_PUSH_NOTIFICATIONS.size()
                    );
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
                    log.info("main rejected push notifications : {}", MAIN_REJECTED_NOTIFICATIONS.size());
                    break;
                case SECONDARY:
                    SECONDARY_REJECTED_NOTIFICATIONS.add(pushNotification);
                    log.info("secondary rejected push notifications : {}", SECONDARY_REJECTED_NOTIFICATIONS.size());
                    break;
                default:
                    throw new UnsupportedOperationException("Apn server type [" + apnServerType + "]is not managed !");
            }
        }

    }

}
