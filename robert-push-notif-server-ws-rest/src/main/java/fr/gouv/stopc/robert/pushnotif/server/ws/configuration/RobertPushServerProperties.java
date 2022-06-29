package fr.gouv.stopc.robert.pushnotif.server.ws.configuration;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Value
@ConstructorBinding
@ConfigurationProperties(prefix = "robert.push.server")
public class RobertPushServerProperties {

    Integer minPushHour;

    Integer maxPushHour;
}
