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
public class PropertyLoader {

    @Min(0)
    @Max(22)
    private Integer minPushHour;

    @Min(1)
    @Max(23)
    private Integer maxPushHour;

    @Positive
    private int maxNumberOfOutstandingNotification;

    // max number of notification that could be push per
    // rateLimitingRefillDurationInSec interval
    @Positive
    private int rateLimitingCapacity;

    // the period within tokens will be fully regenerated ( in seconds)
    // e.g : 100 request per sec will be configured :
    // - rateLimitingCapacity : 100
    // - rateLimitingRefillDurationInSec : 1
    @Positive
    private int rateLimitingRefillDurationInSec;

    @Valid
    private ApnsDefinition apns;

}
