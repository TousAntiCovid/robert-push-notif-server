package fr.gouv.stopc.robert.pushnotif.scheduler;

import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.PushInfoDao;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.mapper.PushInfoRowMapper;
import fr.gouv.stopc.robert.pushnotif.scheduler.dao.model.PushInfo;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class Scheduler {

    private final JdbcTemplate jdbcTemplate;

    private final PushInfoRowMapper rowMapper;

    private final PushInfoDao pushInfoDao;

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsPushNotificationService apnsPushNotificationService;

    @Scheduled(fixedDelayString = "${robert.push.server.scheduler.delay-in-ms}")
    @Timed(value = "push.notifier.duration", description = "on going export duration", longTask = true)
    @Counted(value = "push.notifier.calls", description = "count each time the scheduler sending notifications is triggered")
    public void sendNotifications() throws InterruptedException {

        // use a RowCallBackHandler in order to process a large resultset on a per-row
        // basis.
        jdbcTemplate.query(
                "select * from push where active = true and deleted = false and next_planned_push <= now()",
                new MyRowCallbackHandler(apnsPushNotificationService)
        );

        do {
            log.info(
                    "it remains {} active threads",
                    robertPushServerProperties.getMaxNumberOfOutstandingNotification()
                            - apnsPushNotificationService.getAvailablePermits()
            );
            TimeUnit.SECONDS.sleep(10);
        } while (apnsPushNotificationService.getAvailablePermits() < robertPushServerProperties
                .getMaxNumberOfOutstandingNotification());

    }

    @RequiredArgsConstructor
    private class MyRowCallbackHandler implements RowCallbackHandler {

        private final ApnsPushNotificationService apnsPushNotificationService;

        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
            PushInfo pushInfo = rowMapper.mapRow(resultSet, resultSet.getRow());

            // set the next planned push to be sure the notification could not be sent 2
            // times the same day
            setNextPlannedPushDate(pushInfo);
            pushInfoDao.updateNextPlannedPushDate(pushInfo);

            apnsPushNotificationService.sendPushNotification(pushInfo);
        }
    }

    private void setNextPlannedPushDate(PushInfo push) {
        PushDate pushDate = PushDate.builder()
                .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                .timezone(push.getTimezone())
                .minPushHour(this.robertPushServerProperties.getMinPushHour())
                .maxPushHour(this.robertPushServerProperties.getMaxPushHour())
                .build();

        TimeUtils.getNextPushDate(pushDate).ifPresent(
                o -> push.setNextPlannedPush(
                        LocalDateTime.ofInstant(
                                o.toInstant(), ZoneId
                                        .of("UTC")
                        )
                )
        );
    }

}
