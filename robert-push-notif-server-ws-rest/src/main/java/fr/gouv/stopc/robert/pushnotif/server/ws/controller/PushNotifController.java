package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.server.api.PushTokenApi;
import fr.gouv.stopc.robert.pushnotif.server.api.model.PushRequest;
import fr.gouv.stopc.robert.pushnotif.server.ws.configuration.PushNotifProperties;
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

    private final PushNotifProperties pushNotifProperties;

    @Override
    public ResponseEntity<Void> registerPushToken(final @Valid PushRequest pushRequest) {

        if (!ZoneId.getAvailableZoneIds().contains(pushRequest.getTimezone())) {
            return ResponseEntity.badRequest().build();
        }
        final var dbPushInfos = pushInfoRepository.findByToken(pushRequest.getToken());

        if (dbPushInfos.isPresent()) {
            final var foundPushInfos = dbPushInfos.get();
            if (!foundPushInfos.isActive() || foundPushInfos.isDeleted()) {
                foundPushInfos.setNextPlannedPush(generateDateTomorrowBetween8amAnd7pm(pushRequest.getTimezone()));
            }
            foundPushInfos.setDeleted(false);
            foundPushInfos.setActive(true);
            foundPushInfos.setTimezone(pushRequest.getTimezone());
            foundPushInfos.setLocale(pushRequest.getLocale());
            this.pushInfoRepository.save(foundPushInfos);
        } else {
            this.pushInfoRepository.save(
                    PushInfo.builder()
                            .token(pushRequest.getToken())
                            .locale(pushRequest.getLocale())
                            .timezone(pushRequest.getTimezone())
                            .active(true)
                            .deleted(false)
                            .nextPlannedPush(generateDateTomorrowBetween8amAnd7pm(pushRequest.getTimezone()))
                            .build()
            );
        }
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

    private Instant generateDateTomorrowBetween8amAnd7pm(final String timezone) {
        final Random random = ThreadLocalRandom.current();
        final int durationBetweenHours = pushNotifProperties.getMaxPushHour() - pushNotifProperties.getMinPushHour();
        final LocalDate tomorrowDate = LocalDate.now().plusDays(1);
        return LocalDateTime.of(
                tomorrowDate,
                // TODO: TEST L'ENREGISTREMENT
                LocalTime.of(
                        random.nextInt(durationBetweenHours) + pushNotifProperties.getMinPushHour(), random.nextInt(60)
                )
        ).atZone(ZoneId.of(timezone)).toInstant();
    }
}
