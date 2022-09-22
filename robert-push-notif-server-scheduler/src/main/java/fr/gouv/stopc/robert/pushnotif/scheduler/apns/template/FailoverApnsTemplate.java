package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.util.stream.Collectors.joining;

/**
 * An APNS template able to defer notification sending to fallback servers
 * depending on the reason returned from the previous server.
 */
@Slf4j
@RequiredArgsConstructor
public class FailoverApnsTemplate implements ApnsOperations<FailoverApnsResponseHandler> {

    private final List<ApnsOperations<ApnsResponseHandler>> apnsDelegates;

    @Override
    public void sendNotification(final ApnsPushNotification notification,
            final FailoverApnsResponseHandler responseHandler) {

        final var apnsTemplates = new ConcurrentLinkedQueue<>(apnsDelegates);
        final var first = apnsTemplates.poll();
        if (first != null) {
            first.sendNotification(
                    notification,
                    new TryOnNextServerAfterInactiveResponseHandler(notification, apnsTemplates, responseHandler)
            );
        }
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        apnsDelegates.forEach(apnsTemplate -> apnsTemplate.waitUntilNoActivity(toleranceDuration));
    }

    @Override
    public void close() {
        apnsDelegates.parallelStream().forEach(delegate -> {
            try {
                delegate.close();
            } catch (final Exception e) {
                log.error("Unable to close {} gracefully", delegate, e);
            }
        });
    }

    @Override
    public String toString() {
        final var serverList = apnsDelegates.stream()
                .map(Object::toString)
                .collect(joining(","));
        return String.format("Failover(%s)", serverList);
    }

    @RequiredArgsConstructor
    private static class TryOnNextServerAfterInactiveResponseHandler implements ApnsResponseHandler {

        private final List<RejectionReason> rejectionsHistory = new ArrayList<>();

        private final ApnsPushNotification notification;

        private final ConcurrentLinkedQueue<ApnsOperations<ApnsResponseHandler>> apnsTemplates;

        private final FailoverApnsResponseHandler failoverResponseHandler;

        @Override
        public void onSuccess() {
            failoverResponseHandler.onSuccess();
        }

        @Override
        public void onRejection(final RejectionReason reason) {
            rejectionsHistory.add(reason);
            failoverResponseHandler.onRejection(rejectionsHistory);
        }

        @Override
        public void onError(final Throwable cause) {
            failoverResponseHandler.onError(cause);
        }

        @Override
        public void onInactive(final RejectionReason reason) {
            rejectionsHistory.add(reason);
            final var nextApnsTemplate = apnsTemplates.poll();
            if (null != nextApnsTemplate) {
                // try next apns in the queue
                nextApnsTemplate.sendNotification(notification, this);
            } else {
                failoverResponseHandler.onInactive(rejectionsHistory);
            }
        }
    }
}
