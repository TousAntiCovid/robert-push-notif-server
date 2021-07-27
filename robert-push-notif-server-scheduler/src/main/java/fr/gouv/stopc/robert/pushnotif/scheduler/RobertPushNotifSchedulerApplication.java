package fr.gouv.stopc.robert.pushnotif.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

import java.util.TimeZone;

@SpringBootApplication
public class RobertPushNotifSchedulerApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC")); // It will set UTC timezone
    }

    public static void main(String[] args) {
        SpringApplication.run(RobertPushNotifSchedulerApplication.class, args);
    }

}
