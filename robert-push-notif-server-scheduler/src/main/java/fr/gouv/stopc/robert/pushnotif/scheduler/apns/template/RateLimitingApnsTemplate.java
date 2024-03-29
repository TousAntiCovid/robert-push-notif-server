package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Semaphore;

/**
 * An APNS template decorator to limit notification rate.
 */
@Slf4j
public class RateLimitingApnsTemplate implements ApnsOperations<ApnsResponseHandler> {

    private final LocalBucket rateLimitingBucket;

    private final ApnsOperations<ApnsResponseHandler> delegate;

    private final Semaphore semaphore;

    public RateLimitingApnsTemplate(
            final int maxNotificationsPerSecond,
            final int maxNumberOfPendingNotifications,
            final ApnsOperations<ApnsResponseHandler> delegate) {

        this.delegate = delegate;
        this.semaphore = new Semaphore(maxNumberOfPendingNotifications);

        final var limit = Bandwidth.classic(
                maxNotificationsPerSecond,
                Refill.intervally(
                        maxNotificationsPerSecond,
                        Duration.ofSeconds(1)
                )
        );
        this.rateLimitingBucket = Bucket.builder()
                .addLimit(limit)
                .withMillisecondPrecision()
                .build();
    }

    @Override
    public void sendNotification(final ApnsPushNotification notification,
            final ApnsResponseHandler responseHandler) {

        try {
            semaphore.acquire();
            rateLimitingBucket.asBlocking().consume(1);
        } catch (final InterruptedException e) {
            log.error("error during rate limiting process", e);
            return;
        }
        final var limitedHandler = new ApnsResponseHandler() {

            @Override
            public void onSuccess() {
                semaphore.release();
                responseHandler.onSuccess();
            }

            @Override
            public void onRejection(final RejectionReason reason) {
                semaphore.release();
                responseHandler.onRejection(reason);
            }

            @Override
            public void onInactive(RejectionReason reason) {
                semaphore.release();
                responseHandler.onInactive(reason);
            }

            @Override
            public void onError(final Throwable reason) {
                semaphore.release();
                responseHandler.onError(reason);
            }
        };
        delegate.sendNotification(notification, limitedHandler);
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
        return String.format("RateLimiting(%s)", delegate);
    }
}
