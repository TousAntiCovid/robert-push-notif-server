package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsClientDecorator;
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
    private final List<ApnsClientDecorator> apnsClients;

    public ApnsClientFactory(RobertPushServerProperties robertPushServerProperties, MeterRegistry meterRegistry)
            throws NoSuchAlgorithmException, IOException, InvalidKeyException {
        this.robertPushServerProperties = robertPushServerProperties;
        this.meterRegistry = meterRegistry;
        this.apnsClients = Collections.unmodifiableList(initApnsClient());
    }

    private List<ApnsClientDecorator> initApnsClient()
            throws InvalidKeyException, NoSuchAlgorithmException, IOException {

        var apnsClients = new ArrayList<ApnsClientDecorator>();

        for (RobertPushServerProperties.ApnsClient apnsClientDefinition : robertPushServerProperties.getApns()
                .getClients()) {

            final MicrometerApnsClientMetricsListener listener = new MicrometerApnsClientMetricsListener(
                    meterRegistry, apnsClientDefinition.getHost(), apnsClientDefinition.getPort()
            );

            ApnsClientBuilder apnsClientBuilder = new ApnsClientBuilder()
                    .setApnsServer(apnsClientDefinition.getHost(), apnsClientDefinition.getPort())
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
                    new ApnsClientDecorator(
                            apnsClientBuilder.build(), apnsClientDefinition.getHost(),
                            apnsClientDefinition.getPort()
                    )
            );
        }
        return apnsClients;
    }

}