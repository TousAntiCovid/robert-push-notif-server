package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientMetricsListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a fork of
 * https://github.com/jchambers/pushy/tree/master/micrometer-metrics-listener In
 * order to customize the metrics name.
 */
public class MicrometerApnsClientMetricsListener implements ApnsClientMetricsListener {

    private final Counter writeFailures;

    private final Counter sentNotifications;

    private final Counter acceptedNotifications;

    private final Counter rejectedNotifications;

    private final AtomicInteger openConnections = new AtomicInteger(0);

    private final Counter connectionFailures;

    /**
     * The name of a {@link io.micrometer.core.instrument.Counter} that measures the
     * number of write failures when sending notifications.
     */
    public static final String WRITE_FAILURES_COUNTER_NAME = "pushy.notifications.failed";

    /**
     * The name of a {@link io.micrometer.core.instrument.Counter} that measures the
     * number of notifications sent (regardless of whether they're accepted or
     * rejected by the server).
     */
    public static final String SENT_NOTIFICATIONS_COUNTER_NAME = "pushy.notifications.sent";

    /**
     * The name of a {@link io.micrometer.core.instrument.Counter} that measures the
     * number of notifications accepted by the APNs server.
     */
    public static final String ACCEPTED_NOTIFICATIONS_COUNTER_NAME = "pushy.notifications.accepted";

    /**
     * The name of a {@link io.micrometer.core.instrument.Counter} that measures the
     * number of notifications rejected by the APNs server.
     */
    public static final String REJECTED_NOTIFICATIONS_COUNTER_NAME = "pushy.notifications.rejected";

    /**
     * The name of a {@link io.micrometer.core.instrument.Gauge} that measures the
     * number of open connections in an APNs client's internal connection pool.
     */
    public static final String OPEN_CONNECTIONS_GAUGE_NAME = "pushy.connections.open";

    /**
     * The name of a {@link io.micrometer.core.instrument.Counter} that measures the
     * number of a client's failed connection attempts.
     */
    public static final String CONNECTION_FAILURES_COUNTER_NAME = "pushy.connections.failed";

    /**
     * Constructs a new Micrometer metrics listener that adds metrics to the given
     * registry with the given list of tags.
     *
     * @param meterRegistry the registry to which to add metrics
     * @param host          the apns server host
     * @param port          the apns server port
     */
    public MicrometerApnsClientMetricsListener(final MeterRegistry meterRegistry, String host, int port) {

        Iterable<Tag> tags = Tags.of("host", host, "port", "" + port);

        this.writeFailures = meterRegistry.counter(WRITE_FAILURES_COUNTER_NAME, tags);
        this.sentNotifications = meterRegistry.counter(SENT_NOTIFICATIONS_COUNTER_NAME, tags);
        this.acceptedNotifications = meterRegistry.counter(ACCEPTED_NOTIFICATIONS_COUNTER_NAME, tags);
        this.rejectedNotifications = meterRegistry.counter(REJECTED_NOTIFICATIONS_COUNTER_NAME, tags);

        this.connectionFailures = meterRegistry.counter(CONNECTION_FAILURES_COUNTER_NAME, tags);

        meterRegistry.gauge(OPEN_CONNECTIONS_GAUGE_NAME, tags, openConnections);
    }

    /**
     * Records a failed attempt to send a notification and updates metrics
     * accordingly.
     *
     * @param apnsClient     the client that failed to write the notification; note
     *                       that this is ignored by
     *                       {@code MicrometerApnsClientMetricsListener} instances,
     *                       which should always be used for exactly one client
     * @param notificationId an opaque, unique identifier for the notification that
     *                       could not be written
     */
    @Override
    public void handleWriteFailure(final ApnsClient apnsClient, final long notificationId) {
        this.writeFailures.increment();
    }

    /**
     * Records a successful attempt to send a notification and updates metrics
     * accordingly.
     *
     * @param apnsClient     the client that sent the notification; note that this
     *                       is ignored by
     *                       {@code MicrometerApnsClientMetricsListener} instances,
     *                       which should always be used for exactly one client
     * @param notificationId an opaque, unique identifier for the notification that
     *                       was sent
     */
    @Override
    public void handleNotificationSent(final ApnsClient apnsClient, final long notificationId) {
        this.sentNotifications.increment();
    }

    /**
     * Records that the APNs server accepted a previously-sent notification and
     * updates metrics accordingly.
     *
     * @param apnsClient     the client that sent the accepted notification; note
     *                       that this is ignored by
     *                       {@code MicrometerApnsClientMetricsListener} instances,
     *                       which should always be used for exactly one client
     * @param notificationId an opaque, unique identifier for the notification that
     *                       was accepted
     */
    @Override
    public void handleNotificationAccepted(final ApnsClient apnsClient, final long notificationId) {
        this.acceptedNotifications.increment();
    }

    /**
     * Records that the APNs server rejected a previously-sent notification and
     * updates metrics accordingly.
     *
     * @param apnsClient     the client that sent the rejected notification; note
     *                       that this is ignored by
     *                       {@code MicrometerApnsClientMetricsListener} instances,
     *                       which should always be used for exactly one client
     * @param notificationId an opaque, unique identifier for the notification that
     *                       was rejected
     */
    @Override
    public void handleNotificationRejected(final ApnsClient apnsClient, final long notificationId) {
        this.rejectedNotifications.increment();
    }

    /**
     * Records that the APNs server added a new connection to its internal
     * connection pool and updates metrics accordingly.
     *
     * @param apnsClient the client that added the new connection
     */
    @Override
    public void handleConnectionAdded(final ApnsClient apnsClient) {
        this.openConnections.incrementAndGet();
    }

    /**
     * Records that the APNs server removed a connection from its internal
     * connection pool and updates metrics accordingly.
     *
     * @param apnsClient the client that removed the connection
     */
    @Override
    public void handleConnectionRemoved(final ApnsClient apnsClient) {
        this.openConnections.decrementAndGet();
    }

    /**
     * Records that a previously-started attempt to connect to the APNs server
     * failed and updates metrics accordingly.
     *
     * @param apnsClient the client that failed to connect; note that this is
     *                   ignored by {@code MicrometerApnsClientMetricsListener}
     *                   instances, which should always be used for exactly one
     *                   client
     */
    @Override
    public void handleConnectionCreationFailed(final ApnsClient apnsClient) {
        this.connectionFailures.increment();
    }
}
