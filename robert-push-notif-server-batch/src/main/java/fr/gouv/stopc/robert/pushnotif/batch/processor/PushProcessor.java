package fr.gouv.stopc.robert.pushnotif.batch.processor;

import javax.inject.Inject;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public class PushProcessor implements ItemProcessor<PushInfo, PushInfo> {

    private final IApnsPushNotificationService apnsPushNotifcationService;

    @Inject
    public PushProcessor(IApnsPushNotificationService apnsPushNotifcationService) {

        this.apnsPushNotifcationService = apnsPushNotifcationService;
    }

    @Override
    public PushInfo process(PushInfo push) throws Exception {
        return this.apnsPushNotifcationService.sendPushNotification(push);
    }

}
