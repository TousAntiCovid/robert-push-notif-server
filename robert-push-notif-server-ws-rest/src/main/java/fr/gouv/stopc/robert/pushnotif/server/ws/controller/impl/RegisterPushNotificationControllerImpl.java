package fr.gouv.stopc.robert.pushnotif.server.ws.controller.impl;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import fr.gouv.stopc.robert.pushnotif.server.ws.controller.IRegisterPushNotificationController;
import fr.gouv.stopc.robert.pushnotif.server.ws.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.server.ws.vo.PushInfoVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RegisterPushNotificationControllerImpl implements IRegisterPushNotificationController {

    private final IPushInfoService pushInfoService;
    private final PropertyLoader propertyLoader;

    @Inject
    public RegisterPushNotificationControllerImpl(IPushInfoService pushInfoService,
            PropertyLoader propertyLoader) {

        this.pushInfoService = pushInfoService;
        this.propertyLoader = propertyLoader;

    }

    @Override
    public ResponseEntity register(@Valid PushInfoVo pushInfoVo) {

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

                        push.setDeleted(false);
                        push.setActive(true);
                        push.setTimezone(pushInfoVo.getTimezone());
                        push.setLocale(pushInfoVo.getLocale());
                        push.setNextPlannedPush(nextPlannnedPush);
                        this.pushInfoService.createOrUpdate(push);

                        return ResponseEntity.status(HttpStatus.CREATED).build();
                    }).orElseGet(()-> {
                        log.error("Failed to register to the token due to the previous error(s");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    });
                }).orElseGet(()-> {
                    PushDate pushDate = PushDate.builder()
                            .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                            .timezone(pushInfoVo.getTimezone())
                            .minPushHour(this.propertyLoader.getMinPushHour())
                            .maxPushHour(this.propertyLoader.getMaxPushHour())
                            .build();

                    return TimeUtils.getNextPushDate(pushDate).map(nextPlannnedPush -> {
                        this.pushInfoService.createOrUpdate(PushInfo.builder()
                                .token(pushInfoVo.getToken())
                                .locale(pushInfoVo.getLocale())
                                .timezone(pushInfoVo.getTimezone())
                                .active(true)
                                .deleted(false)
                                .nextPlannedPush(nextPlannnedPush)
                                .build());
                        return ResponseEntity.status(HttpStatus.CREATED).build();
                    }).orElseGet(()-> {
                        log.error("Failed to register to the token due to the previous error(s");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                    });
                });
    }

}
