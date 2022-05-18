package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.server.api.PushTokenApi;
import fr.gouv.stopc.robert.pushnotif.server.api.model.PushRequest;
import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.repository.PushInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Random;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/internal/api/v1")
@RequiredArgsConstructor
public class PushNotifController implements PushTokenApi {

    private final PushInfoRepository pushInfoRepository;

    @Override
    public ResponseEntity<Void> registerPushToken(final @Valid PushRequest pushRequest) {

        if (!ZoneId.getAvailableZoneIds().contains(pushRequest.getTimezone())) {
            return ResponseEntity.badRequest().build();
        }
        final var dbPushInfos = pushInfoRepository.findByToken(pushRequest.getToken());
        dbPushInfos.ifPresentOrElse(
                push -> {
                    if (!push.isActive() || push.isDeleted()) {
                        push.setNextPlannedPush(generateDateTomorrowBetween8amAnd7pm());
                    }
                    push.setDeleted(false);
                    push.setActive(true);
                    push.setTimezone(pushRequest.getTimezone());
                    push.setLocale(pushRequest.getLocale());
                    this.pushInfoRepository.save(push);
                },
                () -> this.pushInfoRepository.save(
                        PushInfo.builder()
                                .token(pushRequest.getToken())
                                .locale(pushRequest.getLocale())
                                .timezone(pushRequest.getTimezone())
                                .active(true)
                                .deleted(false)
                                .nextPlannedPush(generateDateTomorrowBetween8amAnd7pm())
                                .build()
                )
        );
        return ResponseEntity.status(CREATED).build();
    }

    @Override
    public ResponseEntity<Void> unRegisterPushToken(final String token) {
        return this.pushInfoRepository.findByToken(token).map(push -> {
            push.setDeleted(true);
            this.pushInfoRepository.save(push);
            return new ResponseEntity<Void>(ACCEPTED);
        }).orElse(ResponseEntity.status(BAD_REQUEST).build());
    }

    private LocalDateTime generateDateTomorrowBetween8amAnd7pm() {
        return LocalDateTime.of(
                LocalDate.now().plusDays(1),
                LocalTime.of(new Random().nextInt(11) + 8, new Random().nextInt(60))
        );
    }
}
