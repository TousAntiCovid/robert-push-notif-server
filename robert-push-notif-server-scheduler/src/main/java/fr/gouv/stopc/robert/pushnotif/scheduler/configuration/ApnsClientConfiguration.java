package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.MicrometerApnsClientMetricsListener;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsOperations;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsTemplate;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.FailoverApnsTemplate;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.RateLimitingApnsTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApnsClientConfiguration {

    private final RobertPushServerProperties robertPushServerProperties;

    private final MeterRegistry meterRegistry;

    @Bean
    public ArrayList<ApnsOperations> apnsClients()
            throws InvalidKeyException, NoSuchAlgorithmException, IOException {

        var apnsClients = new ArrayList<ApnsOperations>();

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
                    new ApnsTemplate(
                            apnsClientBuilder.build(), apnsClientDefinition.getHost(),
                            apnsClientDefinition.getPort(), robertPushServerProperties
                    )
            );
        }
        return apnsClients;
    }

    @Bean
    public RateLimitingApnsTemplate apnsTemplate(final ArrayList<ApnsOperations> apnsClients) {
        return new RateLimitingApnsTemplate(
                robertPushServerProperties, new FailoverApnsTemplate(apnsClients, robertPushServerProperties)
        );
    }
}
