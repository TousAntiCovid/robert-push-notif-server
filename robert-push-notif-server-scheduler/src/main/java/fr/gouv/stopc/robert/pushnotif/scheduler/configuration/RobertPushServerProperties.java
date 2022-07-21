package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RejectionReason;
import lombok.Builder;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
    int maxNumberOfPendingNotifications;

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

        List<RejectionReason> inactiveRejectionReason;

        @NotNull
        String topic;

        @Valid
        List<ApnsClient> clients;

        Resource trustedClientCertificateChain;

        @Pattern(regexp = "|[^:]+:\\d+")
        String httpProxy;

        public SocketAddress getHttpProxy() {
            final var host = httpProxy.replaceAll(":.*", "");
            final var port = httpProxy.replaceAll(".*:", "");
            return new InetSocketAddress(host, Integer.parseInt(port));
        }
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
