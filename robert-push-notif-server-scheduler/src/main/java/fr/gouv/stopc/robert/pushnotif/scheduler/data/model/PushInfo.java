package fr.gouv.stopc.robert.pushnotif.scheduler.data.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PushInfo {

    private Long id;

    private String token;

    private String timezone;

    private String locale;

    private Instant nextPlannedPush;

    private Instant lastSuccessfulPush;

    private Instant lastFailurePush;

    private String lastErrorCode;

    private int successfulPushSent;

    private int failedPushSent;

    private Instant creationDate;

    private boolean active;

    private boolean deleted;
}
