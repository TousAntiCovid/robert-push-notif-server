package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

@Component
@ConfigurationProperties(prefix = "robert.push.server")
@Validated
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RobertPushServerProperties {

    @Min(0)
    @Max(22)
    private int minPushHour;

    @Min(1)
    @Max(23)
    private int maxPushHour;

    @Positive
    private int maxNumberOfOutstandingNotification;

    @Positive
    private int maxNotificationsPerSecond;

    @Valid
    private ApnsDefinition apns;

}
