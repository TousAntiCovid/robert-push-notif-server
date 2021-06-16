package fr.gouv.stopc.robert.pushnotif.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@ComponentScan(basePackages = "fr.gouv.stopc")
@EnableJpaRepositories("fr.gouv.stopc")
@EntityScan("fr.gouv.stopc")
@SpringBootApplication
public class RobertPushNotifBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobertPushNotifBatchApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {

        return new RestTemplate();
    }
}
