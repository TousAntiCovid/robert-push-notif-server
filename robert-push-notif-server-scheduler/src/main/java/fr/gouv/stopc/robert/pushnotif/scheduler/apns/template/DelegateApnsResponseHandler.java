package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.RequiredArgsConstructor;

/**
 * Base class for {@link ApnsResponseHandler} decorators.
 */
@RequiredArgsConstructor
public class DelegateApnsResponseHandler implements ApnsResponseHandler {

    private final ApnsResponseHandler delegate;

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
    public void onInactive(RejectionReason reason) {
        delegate.onInactive(reason);
    }
}
