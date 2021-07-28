package fr.gouv.stopc.robert.pushnotif.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

import java.time.ZoneOffset;
import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class RobertPushNotifSchedulerApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC)); // It will set UTC timezone
    }

    public static void main(String[] args) {
        SpringApplication.run(RobertPushNotifSchedulerApplication.class, args);
    }

}
