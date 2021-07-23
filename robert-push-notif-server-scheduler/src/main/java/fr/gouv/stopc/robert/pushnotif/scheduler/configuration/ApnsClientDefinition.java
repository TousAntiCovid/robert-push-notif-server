package fr.gouv.stopc.robert.pushnotif.scheduler.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApnsClientDefinition {

    @NotNull
    String host;

    @Positive
    int port;

}
