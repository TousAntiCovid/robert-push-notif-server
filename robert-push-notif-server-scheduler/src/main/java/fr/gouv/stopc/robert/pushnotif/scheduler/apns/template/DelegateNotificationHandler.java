package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;

/**
 * Base class for {@link NotificationHandler} decorators.
 */
@RequiredArgsConstructor
public class DelegateNotificationHandler implements NotificationHandler {

    private final NotificationHandler delegate;

    @Override
    public String getAppleToken() {
        return delegate.getAppleToken();
    }

    @Override
    public void onSuccess() {
        delegate.onSuccess();
    }

    @Override
    public void onRejection(final RejectionReason reason) {
        delegate.onRejection(reason);
    }

    @Override
    public void onError(final Throwable cause) {
        delegate.onError(cause);
    }

    @Override
    public void disableToken() {
        delegate.disableToken();
    }

    @Override
    public ApnsPushNotification buildNotification() {
        return delegate.buildNotification();
    }
}
