package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import com.eatthepath.pushy.apns.ApnsPushNotification;
import lombok.Data;

@Data
public abstract class NotificationPilot<T> {

    protected final T notificationData;

    public abstract String getToken();

    public abstract void updateOnNotificationSuccess();

    public abstract void updateOnNotificationRejection(final String rejectionMessage);

    public abstract void disableToken();

    public abstract ApnsPushNotification buildNotification(final String topic);
}
