package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import fr.gouv.stopc.robert.pushnotif.scheduler.apns.TacApnsClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ApnsClientFactory {

    private final PropertyLoader propertyLoader;

    @Getter
    private List<TacApnsClient> apnsClients;

    public ApnsClientFactory(PropertyLoader propertyLoader)
            throws NoSuchAlgorithmException, IOException, InvalidKeyException {
        this.propertyLoader = propertyLoader;
        initApnsClient();
    }

    private void initApnsClient() throws InvalidKeyException, NoSuchAlgorithmException, IOException {

        this.apnsClients = new ArrayList<>();

        for (ApnsClientDefinition apnsClientDefinition : propertyLoader.getApns().getClients()) {

            ApnsClientBuilder apnsClientBuilder = new ApnsClientBuilder()
                    .setApnsServer(apnsClientDefinition.getHost(), apnsClientDefinition.getPort())
                    .setSigningKey(
                            ApnsSigningKey.loadFromInputStream(
                                    this.propertyLoader.getApns().getAuthTokenFile().getInputStream(),
                                    this.propertyLoader.getApns().getTeamId(),
                                    this.propertyLoader.getApns().getAuthKeyId()
                            )
                    );

            if (propertyLoader.getApns().getTrustedClientCertificateChain() != null) {
                apnsClientBuilder.setTrustedServerCertificateChain(
                        propertyLoader.getApns().getTrustedClientCertificateChain().getInputStream()
                );
            }

            apnsClients.add(
                    new TacApnsClient(
                            apnsClientBuilder.build(), apnsClientDefinition.getHost(),
                            apnsClientDefinition.getPort()
                    )
            );
        }
    }

}
