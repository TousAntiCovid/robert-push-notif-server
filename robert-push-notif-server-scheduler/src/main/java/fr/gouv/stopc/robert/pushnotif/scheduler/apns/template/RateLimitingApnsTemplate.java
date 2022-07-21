package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class RateLimitingApnsTemplate implements ApnsOperations {

    private final LocalBucket rateLimitingBucket;

    private final ApnsOperations delegate;

    public RateLimitingApnsTemplate(
            final int maxNotificationsPerSecond,
            final ApnsOperations delegate) {

        this.delegate = delegate;

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
    public void sendNotification(final NotificationHandler handler) {

        try {
            rateLimitingBucket.asBlocking().consume(1);
        } catch (InterruptedException e) {
            log.error("error during rate limiting process", e);
            return;
        }
        delegate.sendNotification(handler);
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
