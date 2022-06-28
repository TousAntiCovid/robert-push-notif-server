package fr.gouv.stopc.robert.pushnotif.server.ws;

import fr.gouv.stopc.robert.pushnotif.server.ws.configuration.PushNotifProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PushNotifProperties.class)
public class RobertPushNotifWsRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobertPushNotifWsRestApplication.class, args);
    }

}
