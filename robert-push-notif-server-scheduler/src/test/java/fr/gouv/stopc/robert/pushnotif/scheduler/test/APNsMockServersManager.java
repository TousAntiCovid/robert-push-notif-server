package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import org.assertj.core.api.ListAssert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.PRIMARY;
import static fr.gouv.stopc.robert.pushnotif.scheduler.test.APNsMockServersManager.ServerId.SECONDARY;
import static java.util.stream.Collectors.joining;
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
        servers.values().forEach(ApnsMockServerDecorator::clear);
    }

    public static ListAssert<ApnsPushNotification> assertThatNotifsAcceptedBy(final ServerId apnsServerId) {
        final var notifs = new ArrayList<>(servers.get(apnsServerId).getAcceptedPushNotifications());
        return assertThat(notifs)
                .describedAs("Notifications accepted by APNS %s server:\n%s", apnsServerId, describe(notifs));
    }

    public static ListAssert<ApnsPushNotification> assertThatNotifsRejectedBy(final ServerId apnsServerId) {
        final var notifs = new ArrayList<>(servers.get(apnsServerId).getRejectedPushNotifications());
        return assertThat(notifs)
                .describedAs("Notifications rejected by APNS %s server:\n%s", apnsServerId, describe(notifs));
    }

    private static String describe(List<ApnsPushNotification> notifs) {
        return notifs.stream()
                .map(
                        n -> String.format(
                                " - token=%s, topic=%s, pushType=%s, priority=%s, expiration=%s", n.getToken(),
                                n.getTopic(), n.getPushType(), n.getPriority(), n.getExpiration()
                        )
                )
                .collect(joining("\n"));
    }

    public enum ServerId {
        PRIMARY,
        SECONDARY
    }
}
