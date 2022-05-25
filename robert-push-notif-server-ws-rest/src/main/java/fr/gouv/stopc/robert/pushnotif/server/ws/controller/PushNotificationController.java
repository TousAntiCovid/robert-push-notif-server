package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import fr.gouv.stopc.robert.pushnotif.server.ws.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.server.ws.vo.PushInfoVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.ws.rs.Produces;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping(value = { "/internal/api/v1/push-token" })
public class PushNotificationController {

    private final IPushInfoService pushInfoService;

    private final PropertyLoader propertyLoader;

    @PostMapping
    @Produces(APPLICATION_JSON_VALUE)
    public ResponseEntity register(@Valid @RequestBody PushInfoVo pushInfoVo) {

        return this.pushInfoService
                .findByPushToken(pushInfoVo.getToken())
                .map(push -> {

                    PushDate pushDate = PushDate.builder()
                            .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                            .timezone(pushInfoVo.getTimezone())
                            .minPushHour(this.propertyLoader.getMinPushHour())
                            .maxPushHour(this.propertyLoader.getMaxPushHour())
                            .build();

                    return TimeUtils.getNextPushDate(pushDate).map(nextPlannnedPush -> {

                        if (!push.isActive() || push.isDeleted()) {
                            push.setNextPlannedPush(nextPlannnedPush);
                        }

                        push.setDeleted(false);
                        push.setActive(true);
                        push.setTimezone(pushInfoVo.getTimezone());
                        push.setLocale(pushInfoVo.getLocale());
                        this.pushInfoService.createOrUpdate(push);

                        return ResponseEntity.status(HttpStatus.CREATED).build();
                    }).orElseGet(() -> {
                        log.error("Failed to register to the token due to the previous error(s");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    });
                }).orElseGet(() -> {
                    PushDate pushDate = PushDate.builder()
                            .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                            .timezone(pushInfoVo.getTimezone())
                            .minPushHour(this.propertyLoader.getMinPushHour())
                            .maxPushHour(this.propertyLoader.getMaxPushHour())
                            .build();

                    return TimeUtils.getNextPushDate(pushDate).map(nextPlannnedPush -> {
                        this.pushInfoService.createOrUpdate(
                                PushInfo.builder()
                                        .token(pushInfoVo.getToken())
                                        .locale(pushInfoVo.getLocale())
                                        .timezone(pushInfoVo.getTimezone())
                                        .active(true)
                                        .deleted(false)
                                        .nextPlannedPush(nextPlannnedPush)
                                        .build()
                        );
                        return ResponseEntity.status(HttpStatus.CREATED).build();
                    }).orElseGet(() -> {
                        log.error("Failed to register to the token due to the previous error(s");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    });
                });
    }

    @DeleteMapping(path = "/{token}")
    public ResponseEntity unregister(@PathVariable(name = "token") String pushToken) {

        return this.pushInfoService.findByPushToken(pushToken).map(push -> {
            push.setDeleted(true);
            this.pushInfoService.createOrUpdate(push);
            return ResponseEntity.accepted().build();
        }).orElse(ResponseEntity.badRequest().build());
    }

}
