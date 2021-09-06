package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.RateLimitedApnsClient;
import fr.gouv.stopc.robert.pushnotif.scheduler.utils.MicrometerApnsClientMetricsListener;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ApnsClientFactory {

    private final RobertPushServerProperties robertPushServerProperties;

    private final MeterRegistry meterRegistry;

    @Getter
    private final List<RateLimitedApnsClient> apnsClients;

    public ApnsClientFactory(RobertPushServerProperties robertPushServerProperties, MeterRegistry meterRegistry)
            throws NoSuchAlgorithmException, IOException, InvalidKeyException {
        this.robertPushServerProperties = robertPushServerProperties;
        this.meterRegistry = meterRegistry;
        this.apnsClients = Collections.unmodifiableList(initApnsClient());
    }

    private List<RateLimitedApnsClient> initApnsClient() throws InvalidKeyException, NoSuchAlgorithmException, IOException {

        var apnsClients = new ArrayList<RateLimitedApnsClient>();

        for (RobertPushServerProperties.ApnsClient apnsClient : robertPushServerProperties.getApns().getClients()) {

            final MicrometerApnsClientMetricsListener listener = new MicrometerApnsClientMetricsListener(
                    meterRegistry, apnsClient.getHost(), apnsClient.getPort()
            );

            ApnsClientBuilder apnsClientBuilder = new ApnsClientBuilder()
                    .setApnsServer(apnsClient.getHost(), apnsClient.getPort())
                    .setSigningKey(
                            ApnsSigningKey.loadFromInputStream(
                                    this.robertPushServerProperties.getApns().getAuthTokenFile().getInputStream(),
                                    this.robertPushServerProperties.getApns().getTeamId(),
                                    this.robertPushServerProperties.getApns().getAuthKeyId()
                            )
                    )
                    .setMetricsListener(listener);

            if (robertPushServerProperties.getApns().getTrustedClientCertificateChain() != null) {
                apnsClientBuilder.setTrustedServerCertificateChain(
                        robertPushServerProperties.getApns().getTrustedClientCertificateChain().getInputStream()
                );
            }

            apnsClients.add(
                    new RateLimitedApnsClient(
                            apnsClientBuilder.build(), apnsClient.getHost(),
                            apnsClient.getPort()
                    )
            );
        }
        return apnsClients;
    }

}
