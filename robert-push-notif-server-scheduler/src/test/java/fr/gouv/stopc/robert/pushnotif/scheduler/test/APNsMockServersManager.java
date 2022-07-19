package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.FIRST;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.SECOND;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link TestExecutionListener} to start APNs server mocks to be used as a
 * dependency for SpringBootTests.
 */
public class APNsMockServersManager implements TestExecutionListener {

    private static final Map<ServerId, ApnsMockServerDecorator> servers = Map.of(
            FIRST, new ApnsMockServerDecorator(2198),
            SECOND, new ApnsMockServerDecorator(2197)
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
        servers.get(FIRST).clear();
        servers.get(SECOND).clear();
    }

    public static List<ApnsPushNotification> getNotifsAcceptedBySecondServer() {
        return new ArrayList<>(servers.get(SECOND).getAcceptedPushNotifications());
    }

    public static List<ApnsPushNotification> getNotifsAcceptedByMainServer() {
        return new ArrayList<>(servers.get(FIRST).getAcceptedPushNotifications());
    }

    public static void assertThatMainServerAcceptedOne() {
        assertThat(servers.get(FIRST).getAcceptedPushNotifications()).hasSize(1);
    }

    public static void assertThatMainServerAccepted(final int nbNotifications) {
        assertThat(servers.get(FIRST).getAcceptedPushNotifications()).hasSize(nbNotifications);
    }

    public static void assertThatMainServerAcceptedNothing() {
        assertThat(servers.get(FIRST).getAcceptedPushNotifications()).isEmpty();
    }

    public static void assertThatMainServerRejectedOne() {
        assertThat(servers.get(FIRST).getRejectedPushNotifications()).hasSize(1);
    }

    public static void assertThatMainServerRejectedNothing() {
        assertThat(servers.get(FIRST).getRejectedPushNotifications()).isEmpty();
    }

    public static void assertThatSecondServerAcceptedOne() {
        assertThat(servers.get(SECOND).getAcceptedPushNotifications()).hasSize(1);
    }

    public static void assertThatSecondServerAcceptedNothing() {
        assertThat(servers.get(SECOND).getAcceptedPushNotifications()).isEmpty();
    }

    public static void assertThatSecondServerRejectedOne() {
        assertThat(servers.get(SECOND).getRejectedPushNotifications()).hasSize(1);
    }

    public static void assertThatSecondServerRejectedNothing() {
        assertThat(servers.get(SECOND).getRejectedPushNotifications()).isEmpty();
    }

    public enum ServerId {
        FIRST,
        SECOND
    }
}
