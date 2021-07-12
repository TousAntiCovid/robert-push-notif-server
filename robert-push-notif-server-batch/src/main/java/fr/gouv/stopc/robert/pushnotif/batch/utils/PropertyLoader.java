package fr.gouv.stopc.robert.pushnotif.batch.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "robert.push.server")
public class PropertyLoader {

    private Integer minPushHour;

    private Integer maxPushHour;

    private String[] notificationAvailableLanguages;

    private String notificationUrlVersion;

    private String notificationUrl;

    private Resource apnsAuthTokenFile;

    private String apnsAuthKeyId;

    private String apnsTeamId;

    private String apnsHost;

    private String apnsDevelopmentHost;

    private List<String> apnsInactiveRejectionReason;

    private String apnsTopic;

    private boolean pushDateEnable;

    private boolean apnsSecondaryEnable;

    private int batchGridSize;

    private int batchChunkSize;

    private long batchThrottlingPauseInMs;

    private Resource apnsTrustedClientCertificateChain;

    private int apnsMainServerPort;

    private int apnsSecondaryServerPort;

    private int maxWaitingTimeAfterLastSentNotificationInSec;

    private int delayBetweenTwoCheckingAttemptsOfEndedJobInSec;

}
