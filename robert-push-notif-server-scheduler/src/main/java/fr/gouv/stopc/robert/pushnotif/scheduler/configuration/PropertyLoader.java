package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "robert.push.server")
@Validated
@Data
public class PropertyLoader {

    @Min(0)
    @Max(22)
    private Integer minPushHour;

    @Min(1)
    @Max(23)
    private Integer maxPushHour;

    @NotNull
    private int maxNumberOfOutstandingNotification;

    private ApnsDefinition apns;

}
