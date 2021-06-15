package fr.gouv.stopc.robert.pushnotif.batch.processor;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public class PushProcessor implements ItemProcessor<PushInfo, PushInfo> {

    private  IApnsPushNotificationService apnsPushNotifcationService;
    private PropertyLoader propertyLoader;

    @Inject
    public PushProcessor(IApnsPushNotificationService apnsPushNotifcationService, PropertyLoader propertyLoader) {
        
        this.apnsPushNotifcationService = apnsPushNotifcationService;
        this.propertyLoader = propertyLoader;
    }


    @Override
    public PushInfo process(PushInfo push) throws Exception {

        Thread.sleep(propertyLoader.getPushProcessorThrottlingPauseInMs());

        CompletableFuture.runAsync(() -> {
            
            this.apnsPushNotifcationService.sendPushNotification(push);
        });
        
        return push;
    }

}
