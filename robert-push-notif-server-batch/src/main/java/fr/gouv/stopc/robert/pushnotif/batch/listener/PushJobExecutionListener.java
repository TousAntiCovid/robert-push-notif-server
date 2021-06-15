package fr.gouv.stopc.robert.pushnotif.batch.listener;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

@Slf4j
public class PushJobExecutionListener implements JobExecutionListener {

    private final IApnsPushNotificationService apnsPushNotifcationService;

    public PushJobExecutionListener(IApnsPushNotificationService apnsPushNotifcationService) {

        this.apnsPushNotifcationService = apnsPushNotifcationService;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {

    }

    @Override
    public void afterJob(JobExecution jobExecution) {

        // log.info("Trying to close the apns service.");
        // this.apnsPushNotifcationService.close();
    }

}
