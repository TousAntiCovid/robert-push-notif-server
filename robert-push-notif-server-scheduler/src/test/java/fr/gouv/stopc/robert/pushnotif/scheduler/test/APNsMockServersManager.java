package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.PRIMARY;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.SECONDARY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link TestExecutionListener} to start APNs server mocks to be used as a
 * dependency for SpringBootTests.
 */
public class APNsMockServersManager implements TestExecutionListener {

    private static final Map<ServerId, ApnsMockServerDecorator> servers = Map.of(
            PRIMARY, new ApnsMockServerDecorator(2198),
            SECONDARY, new ApnsMockServerDecorator(2197)
    );

    public static void givenApnsServerRejectsTokenIdWith(final ServerId serverId,
            final String token,
            final RejectionReason reason) {
        servers.get(serverId).resetMockWithRejectedToken(token, reason);
    }

    @Override
    public void beforeTestExecution(final TestContext testContext) throws Exception {
        TestExecutionListener.super.beforeTestExecution(testContext);
        servers.values().forEach(ApnsMockServerDecorator::resetMock);
        servers.get(PRIMARY).clear();
        servers.get(SECONDARY).clear();
    }

    public static List<ApnsPushNotification> getNotifsAcceptedBySecondServer() {
        return new ArrayList<>(servers.get(SECONDARY).getAcceptedPushNotifications());
    }

    public static List<ApnsPushNotification> getNotifsAcceptedByMainServer() {
        return new ArrayList<>(servers.get(PRIMARY).getAcceptedPushNotifications());
    }

    public static void assertThatMainServerAcceptedOne() {
        assertThat(servers.get(PRIMARY).getAcceptedPushNotifications()).hasSize(1);
    }

    public static void assertThatMainServerAccepted(final int nbNotifications) {
        assertThat(servers.get(PRIMARY).getAcceptedPushNotifications()).hasSize(nbNotifications);
    }

    public static void assertThatMainServerAcceptedNothing() {
        assertThat(servers.get(PRIMARY).getAcceptedPushNotifications()).isEmpty();
    }

    public static void assertThatMainServerRejectedOne() {
        assertThat(servers.get(PRIMARY).getRejectedPushNotifications()).hasSize(1);
    }

    public static void assertThatMainServerRejectedNothing() {
        assertThat(servers.get(PRIMARY).getRejectedPushNotifications()).isEmpty();
    }

    public static void assertThatSecondServerAcceptedOne() {
        assertThat(servers.get(SECONDARY).getAcceptedPushNotifications()).hasSize(1);
    }

    public static void assertThatSecondServerAcceptedNothing() {
        assertThat(servers.get(SECONDARY).getAcceptedPushNotifications()).isEmpty();
    }

    public static void assertThatSecondServerRejectedOne() {
        assertThat(servers.get(SECONDARY).getRejectedPushNotifications()).hasSize(1);
    }

    public static void assertThatSecondServerRejectedNothing() {
        assertThat(servers.get(SECONDARY).getRejectedPushNotifications()).isEmpty();
    }

    public enum ServerId {
        PRIMARY,
        SECONDARY
    }
}
