package fr.gouv.stopc.robert.pushnotif.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.annotation.PostConstruct;

import java.util.TimeZone;

@EntityScan("fr.gouv.stopc")
@SpringBootApplication
public class RobertPushNotifSchedulerApplication {

    // TODO : FORCER L'APPLICATION A TOURNER EN UTC !!
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // It will set UTC timezone
    }

    public static void main(String[] args) {
        SpringApplication.run(RobertPushNotifSchedulerApplication.class, args);
    }

}
