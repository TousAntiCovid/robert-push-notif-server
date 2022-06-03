package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import java.util.List;

@Getter
@Builder
@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "robert.push.server")
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
    private RobertPushServerProperties.Apns apns;

    @Value
    @Builder
    public static class Apns {

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

    @Value
    @Builder
    public static class ApnsClient {

        @NotNull
        String host;

        @Positive
        int port;

    }
}
