package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.NONE;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

/**
 * An APNS template decorator to exports micrometer metrics about time spent
 * sending each notification and amount of pending notifications beeing sent.
 */
@Slf4j
public class MonitoringApnsTemplate implements ApnsOperations<ApnsResponseHandler> {

    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    private final Map<Tags, Timer> tagsToTimerMap;

    private static final String OUTCOME_TAG_KEY = "outcome";

    private static final String REJECTION_REASON_TAG_KEY = "rejectionReason";

    private final ApnsOperations<ApnsResponseHandler> delegate;

    private final String host;

    private final Integer port;

    public MonitoringApnsTemplate(final ApnsTemplate delegate,
            final ApnsServerCoordinates serverCoordinates,
            final MeterRegistry meterRegistry) {

        this.host = serverCoordinates.getHost();
        this.port = serverCoordinates.getPort();
        this.delegate = delegate;

        final var successTags = Stream.of(
                Tags.of(
                        OUTCOME_TAG_KEY, ACCEPTED.name(),
                        REJECTION_REASON_TAG_KEY, NONE.name()
                )
        );

        final var rejectedTags = Arrays.stream(RejectionReason.values())
                .map(
                        rejectionReason -> Tags.of(
                                OUTCOME_TAG_KEY, REJECTED.name(),
                                REJECTION_REASON_TAG_KEY, rejectionReason.name()
                        )
                );

        final var errorTags = Stream.of(
                Tags.of(
                        OUTCOME_TAG_KEY, ERROR.name(),
                        REJECTION_REASON_TAG_KEY, NONE.name()
                )
        );

        tagsToTimerMap = concat(concat(rejectedTags, successTags), errorTags)
                .map(tags -> tags.and("host", host, "port", port.toString()))
                .collect(
                        toMap(
                                identity(),
                                tags -> Timer.builder("pushy.notifications.sent.timer")
                                        .tags(tags)
                                        .register(meterRegistry)
                        )
                );

        Gauge.builder("pushy.notifications.pending", pendingNotifications::get).register(meterRegistry);
    }

    @Override
    public void sendNotification(final ApnsPushNotification notification,
            final ApnsResponseHandler responseHandler) {

        pendingNotifications.incrementAndGet();

        final var sample = Timer.start();

        final var measuringHandler = new ApnsResponseHandler() {

            @Override
            public void onSuccess() {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(ACCEPTED, NONE));
                responseHandler.onSuccess();
            }

            @Override
            public void onRejection(final RejectionReason reason) {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(REJECTED, reason));
                responseHandler.onRejection(reason);
            }

            @Override
            public void onError(final Throwable cause) {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(ERROR, NONE));
                log.warn("Push Notification sent by {} failed", this, cause);
                responseHandler.onError(cause);
            }

            @Override
            public void onInactive(RejectionReason reason) {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(REJECTED, reason));
                responseHandler.onInactive(reason);
            }
        };

        delegate.sendNotification(notification, measuringHandler);
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        delegate.waitUntilNoActivity(toleranceDuration);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public String toString() {
        return String.format("Monitoring(%s)", delegate);
    }

    /**
     * returns the Timer matching various tags matching parameters.
     *
     * @param outcome         request outcome. Either Accepted, Rejected, or Error.
     * @param rejectionReason rejection code when outcome is Rejected. Otherwise,
     *                        use the value NONE
     * @return
     * @see ApnsRequestOutcome
     * @see RejectionReason
     */
    private Timer getTimer(final ApnsRequestOutcome outcome, final RejectionReason rejectionReason) {
        return tagsToTimerMap.get(
                Tags.of(
                        "host", host,
                        "port", port.toString(),
                        OUTCOME_TAG_KEY, outcome.name(),
                        REJECTION_REASON_TAG_KEY, rejectionReason.name()
                )
        );
    }
}
