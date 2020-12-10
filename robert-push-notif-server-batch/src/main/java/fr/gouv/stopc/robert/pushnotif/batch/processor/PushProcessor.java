package fr.gouv.stopc.robert.pushnotif.batch.processor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.springframework.batch.item.ItemProcessor;

import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.apns.service.impl.ApnsPushNotificationServiceImpl;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PushProcessor implements ItemProcessor<PushInfo, PushInfo> {

    private  IApnsPushNotificationService apnsPushNotifcationService;
    
    PropertyLoader propertyLoader;
    private IPushInfoService pushInfoService;

//    @Inject
    public PushProcessor(IApnsPushNotificationService apnsPushNotifcationService) {
        
        this.apnsPushNotifcationService = apnsPushNotifcationService;
    }

    public PushProcessor(PropertyLoader propertyLoader, IPushInfoService pushInfoService) {

        this.propertyLoader = propertyLoader;
        this.pushInfoService = pushInfoService;
        
    }

    @Override
    public PushInfo process(PushInfo push) throws Exception {
//        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture = 
        CompletableFuture.runAsync(() -> {
            
//            IApnsPushNotificationService localApnsPushNotifcationService = new ApnsPushNotificationServiceImpl(this.propertyLoader, this.pushInfoService);
            this.apnsPushNotifcationService.sendPushNotification(push);
        });
        
//        sendNotificationFuture.thenAcceptAsync((response) -> {
//            if (Objects.nonNull(response)) {
//                // Handle the push notification response as before from here.             
//                log.info("Push Notification successful sent => {}", response);
//            } else {
//                // Something went wrong when trying to send the notification to the
//                // APNs server. Note that this is distinct from a rejection from
//                // the server, and indicates that something went wrong when actually
//                // sending the notification or waiting for a reply.
//                log.info("Push Notification failed => {}", response);
//            }
//        });
        
        return push;
    }

}
