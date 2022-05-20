package fr.gouv.stopc.robert.pushnotif.server.ws.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "robert.push.server")
public class PushNotifProperties {

    private Integer minPushHour;

    private Integer maxPushHour;
}
