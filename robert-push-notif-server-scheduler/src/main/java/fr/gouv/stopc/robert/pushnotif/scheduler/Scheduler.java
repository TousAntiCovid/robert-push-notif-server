package fr.gouv.stopc.robert.pushnotif.scheduler;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.PushInfoRowMapper;
import fr.gouv.stopc.robert.pushnotif.scheduler.data.model.PushInfo;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final JdbcTemplate jdbcTemplate;

    private final PushInfoRowMapper rowMapper = new PushInfoRowMapper();

    private final PushInfoDao pushInfoDao;

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsPushNotificationService apnsPushNotificationService;

    @Scheduled(fixedDelayString = "${robert.push.server.scheduler.delay-in-ms}")
    @Timed(value = "push.notifier.duration", description = "on going export duration", longTask = true)
    @Counted(value = "push.notifier.calls", description = "count each time the scheduler sending notifications is triggered")
    public void sendNotifications() {

        // use a RowCallBackHandler in order to process a large resultset on a per-row
        // basis.
        jdbcTemplate.query(
                "select * from push where active = true and deleted = false and next_planned_push <= now()",
                new PushNotificationRowCallbackHandler(apnsPushNotificationService)
        );

        apnsPushNotificationService.waitUntilNoActivity(Duration.ofSeconds(10));
    }

    @RequiredArgsConstructor
    private class PushNotificationRowCallbackHandler implements RowCallbackHandler {

        private final ApnsPushNotificationService apnsPushNotificationService;

        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
            PushInfo pushInfo = rowMapper.mapRow(resultSet, resultSet.getRow());

            // set the next planned push to be sure the notification could not be sent 2
            // times the same day
            assert pushInfo != null;
            pushInfo.setNextPlannedPush(generateDateTomorrowBetweenBounds(pushInfo.getTimezone()));

            pushInfoDao.updateNextPlannedPushDate(pushInfo);

            apnsPushNotificationService.sendPushNotification(pushInfo);
        }
    }

    private Instant generateDateTomorrowBetweenBounds(final String timezone) {

        final Random random = ThreadLocalRandom.current();
        final int maxPushHour = robertPushServerProperties.getMaxPushHour();
        final int minPushHour = robertPushServerProperties.getMinPushHour();

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
