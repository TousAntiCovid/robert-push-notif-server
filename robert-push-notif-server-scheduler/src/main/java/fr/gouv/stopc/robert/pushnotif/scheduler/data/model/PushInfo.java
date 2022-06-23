package fr.gouv.stopc.robert.pushnotif.scheduler.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushInfo {

    Long id;

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
