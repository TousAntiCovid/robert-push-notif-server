package fr.gouv.stopc.robert.pushnotif.server.ws.controller;

import fr.gouv.stopc.robert.pushnotif.server.api.PushTokenApi;
import fr.gouv.stopc.robert.pushnotif.server.api.model.PushRequest;
import fr.gouv.stopc.robert.pushnotif.server.ws.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.server.ws.repository.PushInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/internal/api/v1")
@RequiredArgsConstructor
public class PushNotifController implements PushTokenApi {

    private final PushInfoRepository pushInfoRepository;

    private final RobertPushServerProperties robertPushServerProperties;

    @Override
    public ResponseEntity<Void> registerPushToken(final @Valid PushRequest pushRequest) {

        if (!ZoneId.getAvailableZoneIds().contains(pushRequest.getTimezone())) {
            return ResponseEntity.badRequest().build();
        }
        final var dbPushInfos = pushInfoRepository.findByToken(pushRequest.getToken());

        if (dbPushInfos.isPresent()) {
            final var foundPushInfos = dbPushInfos.get();
            if (!foundPushInfos.isActive() || foundPushInfos.isDeleted()) {
                foundPushInfos.setNextPlannedPush(generateDateTomorrowBetweenBounds(pushRequest.getTimezone()));
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
                            .nextPlannedPush(generateDateTomorrowBetweenBounds(pushRequest.getTimezone()))
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

    private Instant generateDateTomorrowBetweenBounds(final String timezone) {

        final Random random = ThreadLocalRandom.current();
        final Integer maxPushHour = robertPushServerProperties.getMaxPushHour();
        final Integer minPushHour = robertPushServerProperties.getMinPushHour();

        final int durationBetweenHours;
        // In case config requires "between 6pm and 4am" which translates in minPushHour
        // = 18 and maxPushHour = 4
        if (maxPushHour < minPushHour) {
            durationBetweenHours = 24 - minPushHour + maxPushHour;
        } else {
            durationBetweenHours = maxPushHour - minPushHour;
        }

        return ZonedDateTime.now(ZoneId.of(timezone)).plusDays(1)
                .withHour(random.nextInt(durationBetweenHours) + minPushHour % 24)
                .withMinute(random.nextInt(60))
                .toInstant()
                .truncatedTo(MINUTES);
    }
}
