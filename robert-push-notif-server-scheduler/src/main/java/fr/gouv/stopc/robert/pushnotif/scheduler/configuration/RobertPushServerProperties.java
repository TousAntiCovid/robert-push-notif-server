package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.Builder;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import java.util.List;

@Value
@Builder
@ConstructorBinding
@ConfigurationProperties(prefix = "robert.push.server")
public class RobertPushServerProperties {

    @Min(0)
    @Max(22)
    int minPushHour;

    @Min(1)
    @Max(23)
    int maxPushHour;

    @Positive
    int maxNotificationsPerSecond;

    @Valid
    RobertPushServerProperties.Apns apns;

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
