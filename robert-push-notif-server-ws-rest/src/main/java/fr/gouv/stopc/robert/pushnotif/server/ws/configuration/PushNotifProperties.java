package fr.gouv.stopc.robert.pushnotif.server.ws.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties(prefix = "robert.push.server")
public class PushNotifProperties {

    private Integer minPushHour;

    private Integer maxPushHour;
}
