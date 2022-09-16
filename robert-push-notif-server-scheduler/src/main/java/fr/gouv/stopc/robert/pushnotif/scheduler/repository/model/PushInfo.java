package fr.gouv.stopc.robert.pushnotif.scheduler.repository.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PushInfo {

    Long id;

    String token;

    String timezone;
}
