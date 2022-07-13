package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.MetricsService;
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

import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApnsClientConfiguration {

    private final RobertPushServerProperties robertPushServerProperties;

    private final MeterRegistry meterRegistry;

    private final MetricsService metricsService;

    private ApnsOperations buildApnsTemplate(final RobertPushServerProperties.ApnsClient apnsClientProperties) {

        final var listener = new MicrometerApnsClientMetricsListener(
                meterRegistry,
                apnsClientProperties.getHost(),
                apnsClientProperties.getPort()
        );

        try (final var authTokenFile = this.robertPushServerProperties.getApns().getAuthTokenFile().getInputStream()) {
            final var apnsClientBuilder = new ApnsClientBuilder()
                    .setApnsServer(apnsClientProperties.getHost(), apnsClientProperties.getPort())
                    .setSigningKey(
                            ApnsSigningKey.loadFromInputStream(
                                    authTokenFile,
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

            return new ApnsTemplate(
                    apnsClientBuilder.build(),
                    metricsService,
                    apnsClientProperties.getHost(),
                    apnsClientProperties.getPort(),
                    meterRegistry
            );
        } catch (final IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(
                    "Unable to open authTokenFile: " + robertPushServerProperties.getApns().getAuthTokenFile(), e
            );
        }
    }

    @Bean
    public ApnsOperations apnsTemplate() {
        final var apnsClients = robertPushServerProperties.getApns().getClients().stream()
                .map(this::buildApnsTemplate)
                .collect(toUnmodifiableList());

        return new RateLimitingApnsTemplate(
                robertPushServerProperties.getMaxNotificationsPerSecond(),
                new FailoverApnsTemplate(apnsClients, robertPushServerProperties.getApns().getInactiveRejectionReason())
        );
    }
}
