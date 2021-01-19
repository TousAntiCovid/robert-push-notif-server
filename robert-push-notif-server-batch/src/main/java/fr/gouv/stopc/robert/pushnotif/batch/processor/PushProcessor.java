package fr.gouv.stopc.robert.pushnotif.batch.processor;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public class PushProcessor implements ItemProcessor<PushInfo, PushInfo> {

    private  IApnsPushNotificationService apnsPushNotifcationService;

    @Inject
    public PushProcessor(IApnsPushNotificationService apnsPushNotifcationService) {
        
        this.apnsPushNotifcationService = apnsPushNotifcationService;
    }


    @Override
    public PushInfo process(PushInfo push) throws Exception {
        CompletableFuture.runAsync(() -> {
            
            this.apnsPushNotifcationService.sendPushNotification(push);
        });
        
        return push;
    }

}
