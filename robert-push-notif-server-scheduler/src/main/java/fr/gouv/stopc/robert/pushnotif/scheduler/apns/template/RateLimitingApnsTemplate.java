package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.NotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
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

    final RobertPushServerProperties robertPushServerProperties;

    public RateLimitingApnsTemplate(
            final RobertPushServerProperties robertPushServerProperties,
            final ApnsOperations apnDelegate) {

        this.apnDelegate = apnDelegate;
        this.robertPushServerProperties = robertPushServerProperties;

        final var limit = Bandwidth.classic(
                robertPushServerProperties.getMaxNotificationsPerSecond(),
                Refill.intervally(
                        robertPushServerProperties.getMaxNotificationsPerSecond(),
                        Duration.ofSeconds(1)
                )
        );
        final var configuration = Bucket4j.configurationBuilder().addLimit(limit).build();
        this.rateLimitingBucket = new LockFreeBucket(configuration, TimeMeter.SYSTEM_MILLISECONDS);
    }

    @Override
    public <T> void sendNotification(NotificationHandler<T> handler) {

        try {
            rateLimitingBucket.consume(1);
        } catch (InterruptedException e) {
            log.error("error during rate limiting process", e);
            return;
        }

        apnDelegate.sendNotification(handler);
    }

    @Override
    public void waitUntilNoActivity(Duration toleranceDuration) {
        apnDelegate.waitUntilNoActivity(toleranceDuration);
    }
}
