package fr.gouv.stopc.robert.pushnotif.batch.listener;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static fr.gouv.stopc.robert.pushnotif.batch.utils.PushBatchConstants.MAIN_STEP_NAME;

@Slf4j
public class PushJobExecutionListener implements JobExecutionListener {

    private final IApnsPushNotificationService apnsPushNotificationService;

    private final IPushInfoService pushInfoService;

    private final PropertyLoader propertyLoader;

    public PushJobExecutionListener(IApnsPushNotificationService apnsPushNotificationService,
            IPushInfoService pushInfoService,
            PropertyLoader propertyLoader) {

        this.apnsPushNotificationService = apnsPushNotificationService;
        this.pushInfoService = pushInfoService;
        this.propertyLoader = propertyLoader;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        try {
            log.info("this job is initialized with {}", propertyLoader.toString());
            apnsPushNotificationService.initApnsClient();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("An error occurred during init of APNs client(s)", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {

        try {
            StepExecution mainStep = jobExecution.getStepExecutions().stream()
                    .filter(s -> s.getStepName().equalsIgnoreCase(MAIN_STEP_NAME)).findFirst().get();
            if (mainStep.getReadCount() > 0) {
                log.info("begin of await sending of all notifications");
                awaitSendingOfAllNotification();
                log.info("end of await sending of all notifications");
            }
            apnsPushNotificationService.close();
        } catch (ExecutionException | InterruptedException e) {
            log.error("An error occurred during closure of APNs client(s)", e);
            throw new RuntimeException(e);
        }
    }

    private void awaitSendingOfAllNotification() throws InterruptedException {

        boolean doesAllNotificationSent = false;

        while (!doesAllNotificationSent) {

            Optional<Date> lastSuccessfulDate = pushInfoService.findMaxLastSuccessfulPush();
            Optional<Date> lastFailureDate = pushInfoService.findMaxLastFailurePush();

            Optional<Date> lastDate = Arrays.asList(lastFailureDate, lastSuccessfulDate).stream()
                    .filter(Optional::isPresent).map(o -> o.get()).max(Date::compareTo);

            if (lastDate.isPresent() &&
                    Instant.ofEpochMilli(TimeUtils.getNowAtTimeZoneUTC().getTime()).isAfter(
                            Instant.ofEpochMilli(lastDate.get().getTime())
                                    .plusSeconds(propertyLoader.getMaxWaitingTimeAfterLastSentNotificationInSec())
                    )) {
                doesAllNotificationSent = true;
            } else {
                TimeUnit.SECONDS.sleep(propertyLoader.getDelayBetweenTwoCheckingAttemptsOfEndedJobInSec());
            }
        }
    }

}
