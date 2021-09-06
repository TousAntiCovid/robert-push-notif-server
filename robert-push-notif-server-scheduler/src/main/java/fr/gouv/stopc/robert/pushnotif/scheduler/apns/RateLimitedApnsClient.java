package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsPushNotification;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import io.github.bucket4j.BlockingBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class RateLimitedApnsClient {

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsClient apnsClient;

    private final String host;

    private final int port;

    private final Semaphore semaphore;

    private final BlockingBucket rateLimitingBucket;

    public PushNotification sendNotification(final ApnsPushNotification notification) {
        try {
            rateLimitingBucket.consume(1);
            semaphore.acquire();

            final var notifFuture = apnsClient.sendNotification(notification)
                    .handle((response, cause) -> {
                        semaphore.release();
                        return response;
                    });
            return new PushNotification(notifFuture);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getId() {
        return host + "[" + port + "]";
    }

    @RequiredArgsConstructor
    public class PushNotification {

        private final CompletableFuture<PushNotificationResponse<ApnsPushNotification>> response;

        public PushNotification onComplete(Consumer<String> successCallback,
                Consumer<RejectedReasonCategory> failureCallback) {
            response.exceptionally(e -> {
                log.error("Unexpected error occurred", e);
                return null;
            }).whenComplete((response, cause) -> {
                if (null == response) {
                    // Something went wrong when trying to send the notification to the
                    // APNs server. Note that this is distinct from a rejection from
                    // the server, and indicates that something went wrong when actually
                    // sending the notification or waiting for a reply.
                    failureCallback.accept(RejectedReasonCategory.OTHER);
                } else if (response.isAccepted()) {
                    successCallback.accept(response.getApnsId().toString());
                } else {
                    final var inactiveRejectedReasons = RateLimitedApnsClient.this.robertPushServerProperties.getApns()
                            .getInactiveRejectionReason();
                    if (inactiveRejectedReasons.contains(response.getRejectionReason())) {
                        failureCallback.accept(RejectedReasonCategory.INACTIVE);
                    } else {
                        failureCallback.accept(RejectedReasonCategory.OTHER);
                    }
                }
            });
            return this;
        }
    }

    public enum RejectedReasonCategory {
        INACTIVE,
        OTHER
    }
}
