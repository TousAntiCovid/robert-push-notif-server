package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.server.ws.utils.UriConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V1 + UriConstants.PATH })
public interface IUnregisterPushNotificationController {

    @DeleteMapping(path = UriConstants.TOKEN_PATH_VARIABLE)
    ResponseEntity unregister(@PathVariable(name = "token") String pushToken);
}
