package fr.gouv.stopc.robert.pushnotif.server.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@ComponentScan(basePackages  = "fr.gouv.stopc")
@EnableJpaRepositories("fr.gouv.stopc")
@EntityScan("fr.gouv.stopc")
@SpringBootApplication
public class RobertPushNotifWsRestApplication {

	public static void main(String[] args) {
		SpringApplication.run(RobertPushNotifWsRestApplication.class, args);
	}

}
