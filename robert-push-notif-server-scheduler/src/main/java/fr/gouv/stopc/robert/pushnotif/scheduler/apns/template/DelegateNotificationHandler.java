package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;

/**
 * Base class for {@link ApnsNotificationHandler} decorators.
 */
@RequiredArgsConstructor
public class DelegateNotificationHandler implements ApnsNotificationHandler {

    private final ApnsNotificationHandler delegate;

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
}
