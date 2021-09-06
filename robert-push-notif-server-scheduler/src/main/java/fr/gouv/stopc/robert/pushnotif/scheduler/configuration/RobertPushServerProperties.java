package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import java.util.List;

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

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApnsDefinition {

        @NotNull
        Resource authTokenFile;

        @NotNull
        String authKeyId;

        @NotNull
        String teamId;

        List<String> inactiveRejectionReason;

        @NotNull
        String topic;

        @Valid
        List<ApnsClient> clients;

        Resource trustedClientCertificateChain;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApnsClient {

        @NotNull
        String host;

        @Positive
        int port;

    }
}
