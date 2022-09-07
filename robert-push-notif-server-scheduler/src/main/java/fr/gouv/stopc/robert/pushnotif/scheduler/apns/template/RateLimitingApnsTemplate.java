package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * An APNS template decorator to limit notification rate.
 */
@Slf4j
public class RateLimitingApnsTemplate implements ApnsOperations {

    private final LocalBucket rateLimitingBucket;

    private final ApnsOperations delegate;

    private final Semaphore semaphore;

    @Override
    public String getName() {
        return delegate.getName();
    }

    public RateLimitingApnsTemplate(
            final int maxNotificationsPerSecond,
            final int maxNumberOfPendingNotifications,
            final ApnsOperations delegate) {

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
    public void sendNotification(final NotificationHandler notificationHandler, final List<String> rejections) {

        try {
            semaphore.acquire();
            rateLimitingBucket.asBlocking().consume(1);
        } catch (InterruptedException e) {
            log.error("error during rate limiting process", e);
            return;
        }
        final var limitedHandler = new DelegateNotificationHandler(notificationHandler) {

            @Override
            public void onSuccess() {
                semaphore.release();
                super.onSuccess();
            }

            @Override
            public void onRejection(final RejectionReason reason, final List<String> pastRejections) {
                semaphore.release();
                super.onRejection(reason, pastRejections);
            }

            @Override
            public void onError(final Throwable reason) {
                semaphore.release();
                super.onError(reason);
            }
        };
        delegate.sendNotification(limitedHandler, rejections);
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        delegate.waitUntilNoActivity(toleranceDuration);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
