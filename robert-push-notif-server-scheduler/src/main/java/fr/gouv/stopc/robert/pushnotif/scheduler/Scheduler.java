package fr.gouv.stopc.robert.pushnotif.scheduler;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.PushInfoNotificationHandler;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsOperations;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class Scheduler {

    private final JdbcTemplate jdbcTemplate;

    private final PushInfoRowMapper rowMapper = new PushInfoRowMapper();

    private final PushInfoDao pushInfoDao;

    private final RobertPushServerProperties robertPushServerProperties;

    private final ApnsOperations apnsTemplate;

    @Scheduled(fixedDelayString = "${robert.push.server.scheduler.delay-in-ms}")
    @Timed(value = "push.notifier.duration", description = "on going export duration", longTask = true)
    @Counted(value = "push.notifier.calls", description = "count each time the scheduler sending notifications is triggered")
    public void sendNotifications() {

        // use a RowCallBackHandler in order to process a large resultset on a per-row
        // basis.
        jdbcTemplate.query(
                "select * from push where active = true and deleted = false and next_planned_push <= now()",
                new PushNotificationRowCallbackHandler()
        );

        apnsTemplate.waitUntilNoActivity(Duration.ofSeconds(10));
    }

    @RequiredArgsConstructor
    private class PushNotificationRowCallbackHandler implements RowCallbackHandler {

        @Override
        public void processRow(final ResultSet resultSet) throws SQLException {
            PushInfo pushInfo = rowMapper.mapRow(resultSet, resultSet.getRow());

            // set the next planned push to be sure the notification could not be sent 2
            // times the same day
            PushInfoNotificationHandler handler = new PushInfoNotificationHandler(
                    pushInfo,
                    pushInfoDao,
                    robertPushServerProperties.getApns().getTopic(),
                    robertPushServerProperties.getMinPushHour(),
                    robertPushServerProperties.getMaxPushHour()
            );

            handler.updateNextPlannedPushToRandomTomorrow();

            apnsTemplate.sendNotification(handler);
        }
    }
}
