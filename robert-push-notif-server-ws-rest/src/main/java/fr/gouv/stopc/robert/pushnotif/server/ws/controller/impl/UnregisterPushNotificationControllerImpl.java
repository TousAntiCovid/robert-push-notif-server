package fr.gouv.stopc.robert.pushnotif.server.ws.controller.impl;

import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import fr.gouv.stopc.robert.pushnotif.server.ws.controller.IUnregisterPushNotificationController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UnregisterPushNotificationControllerImpl implements IUnregisterPushNotificationController {

    private final IPushInfoService pushInfoService;

    public UnregisterPushNotificationControllerImpl(IPushInfoService pushInfoService) {

        this.pushInfoService = pushInfoService;
    }

    @Override
    public ResponseEntity unregister(String pushToken) {

        return this.pushInfoService.findByPushToken(pushToken).map(push -> {
            push.setDeleted(true);
            this.pushInfoService.createOrUpdate(push);
            return ResponseEntity.accepted().build();
        }).orElse(ResponseEntity.badRequest().build());
    }

}
