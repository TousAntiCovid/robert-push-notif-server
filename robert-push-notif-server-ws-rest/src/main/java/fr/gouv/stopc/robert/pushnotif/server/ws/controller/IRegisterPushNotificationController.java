package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.stopc.robert.pushnotif.server.ws.utils.UriConstants;
import fr.gouv.stopc.robert.pushnotif.server.ws.vo.PushInfoVo;


@RestController
@RequestMapping(value = {"${controller.path.prefix}" + UriConstants.API_V1 + UriConstants.PATH})
@Consumes(MediaType.APPLICATION_JSON_VALUE)
@Produces(MediaType.APPLICATION_JSON_VALUE)
public interface IRegisterPushNotificationController {

    @PostMapping
    ResponseEntity register(@Valid @RequestBody(required = true) PushInfoVo pushInfo);

}
