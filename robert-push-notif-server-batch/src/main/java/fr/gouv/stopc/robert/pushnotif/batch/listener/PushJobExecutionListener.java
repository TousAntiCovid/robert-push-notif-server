package fr.gouv.stopc.robert.pushnotif.batch.listener;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

@Slf4j
public class PushJobExecutionListener implements JobExecutionListener {

    private final IApnsPushNotificationService apnsPushNotifcationService;

    public PushJobExecutionListener(IApnsPushNotificationService apnsPushNotifcationService) {

        this.apnsPushNotifcationService = apnsPushNotifcationService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        try {
            apnsPushNotifcationService.initApnsClient();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("An error occurred during init of APNs client(s)", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {

        try {
            apnsPushNotifcationService.close();
        } catch (ExecutionException | InterruptedException e) {
            log.error("An error occurred during init of APNs client(s)", e);
            throw new RuntimeException(e);
        }
    }

}
