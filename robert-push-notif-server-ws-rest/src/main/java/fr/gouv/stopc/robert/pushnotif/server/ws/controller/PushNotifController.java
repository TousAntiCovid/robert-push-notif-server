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

import java.time.*;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

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
                foundPushInfos -> {
                    if (!foundPushInfos.isActive() || foundPushInfos.isDeleted()) {
                        foundPushInfos.setNextPlannedPush(generateDateTomorrowBetween8amAnd7pm());
                    }
                    foundPushInfos.setDeleted(false);
                    foundPushInfos.setActive(true);
                    foundPushInfos.setTimezone(pushRequest.getTimezone());
                    foundPushInfos.setLocale(pushRequest.getLocale());
                    this.pushInfoRepository.save(foundPushInfos);
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

    private Instant generateDateTomorrowBetween8amAnd7pm() {
        Random random = ThreadLocalRandom.current();
        return LocalDateTime.of(
                LocalDate.now().plusDays(1),
                LocalTime.of(random.nextInt(11) + 8, random.nextInt(60))
        ).toInstant(ZoneOffset.UTC);
    }
}
