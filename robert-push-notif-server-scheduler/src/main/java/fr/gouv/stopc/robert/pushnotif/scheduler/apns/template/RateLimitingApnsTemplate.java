package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.NotificationHandler;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
import io.github.bucket4j.local.LockFreeBucket;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class RateLimitingApnsTemplate implements ApnsOperations {

    private final BlockingBucket rateLimitingBucket;

    private final ApnsOperations apnDelegate;

    public RateLimitingApnsTemplate(
            final int maxNotificationsPerSecond,
            final ApnsOperations apnDelegate) {

        this.apnDelegate = apnDelegate;

        final var limit = Bandwidth.classic(
                maxNotificationsPerSecond,
                Refill.intervally(
                        maxNotificationsPerSecond,
                        Duration.ofSeconds(1)
                )
        );
        final var configuration = Bucket4j.configurationBuilder().addLimit(limit).build();
        this.rateLimitingBucket = new LockFreeBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    @Override
    public <T> void sendNotification(final NotificationHandler handler) {

        try {
            rateLimitingBucket.consume(1);
        } catch (InterruptedException e) {
            log.error("error during rate limiting process", e);
            return;
        }
        apnDelegate.sendNotification(handler);
    }

    @Override
    public void waitUntilNoActivity(final Duration toleranceDuration) {
        apnDelegate.waitUntilNoActivity(toleranceDuration);
    }
}
