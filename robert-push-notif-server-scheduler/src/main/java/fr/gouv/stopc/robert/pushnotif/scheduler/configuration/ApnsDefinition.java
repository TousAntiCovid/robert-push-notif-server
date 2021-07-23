package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.Data;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.List;

@Data
public class ApnsDefinition {

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
    List<ApnsClientDefinition> clients;

    Resource trustedClientCertificateChain;

}
