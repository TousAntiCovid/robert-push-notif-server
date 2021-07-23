package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class ApnsClientDefinition {

    @NotNull
    String host;

    @Positive
    int port;

}
