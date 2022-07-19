package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.*;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason.NONE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
public class MonitoringApnsTemplate implements ApnsOperations {

    /**
     * This map contains timers with their varying tags as keys. Using tags as keys
     * allows us to have a variable tags structure: for example, Success outcome
     * timers do not possess any rejectionReason. the current varying tags are:
     * outcome host port rejectionReason
     */

    private final AtomicInteger pendingNotifications = new AtomicInteger(0);

    private final Map<Tags, Timer> tagsToTimerMap;

    private final ApnsOperations delegate;

    @ToString.Include
    private final String host;

    @ToString.Include
    private final Integer port;

    public MonitoringApnsTemplate(final ApnsTemplate delegate,
            final String host,
            final Integer port,
            final MeterRegistry meterRegistry) {

        this.host = host;
        this.port = port;
        this.delegate = delegate;

        final var successTags = Stream.of(
                Tags.of(
                        "outcome", ACCEPTED.name(),
                        "rejectionReason", NONE.name()
                )
        );

        final var rejectedTags = Arrays.stream(RejectionReason.values())
                .map(
                        rejectionReason -> Tags.of(
                                "outcome", REJECTED.name(),
                                "rejectionReason", rejectionReason.name()
                        )
                );

        final var errorTags = Stream.of(
                Tags.of(
                        "outcome", ERROR.name(),
                        "rejectionReason", NONE.name()
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
    public <T> void sendNotification(final NotificationHandler handler) {

        pendingNotifications.incrementAndGet();

        final var sample = Timer.start();

        final var measuringHandler = new NotificationHandler() {

            @Override
            public String getAppleToken() {
                return handler.getAppleToken();
            }

            @Override
            public void onSuccess() {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(ACCEPTED, NONE));
                handler.onSuccess();
            }

            @Override
            public void onRejection(final RejectionReason rejectionMessage) {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(REJECTED, rejectionMessage));
                handler.onRejection(rejectionMessage);
            }

            @Override
            public void onError(final Throwable cause) {
                pendingNotifications.decrementAndGet();
                sample.stop(getTimer(ERROR, NONE));
                log.warn("Push Notification sent by {} failed", this, cause);
                handler.onError(cause);
            }

            @Override
            public void disableToken() {
                handler.disableToken();
            }

            @Override
            public ApnsPushNotification buildNotification() {
                return handler.buildNotification();
            }
        };

        delegate.sendNotification(measuringHandler);
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        do {
            delegate.waitUntilNoActivity(toleranceDuration);
            try {
                SECONDS.sleep(toleranceDuration.getSeconds());
            } catch (InterruptedException e) {
                log.warn("Unable to wait until all notifications are sent", e);
            }
        } while (pendingNotifications.get() != 0);
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down {} -----> shutting down delegate: {}", this, delegate);
        delegate.close();
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
    public Timer getTimer(final ApnsRequestOutcome outcome,
            final RejectionReason rejectionReason) {
        return tagsToTimerMap.get(
                Tags.of(
                        "host", host,
                        "port", port.toString(),
                        "outcome", outcome.name(),
                        "rejectionReason", rejectionReason.name()
                )
        );
    }
}