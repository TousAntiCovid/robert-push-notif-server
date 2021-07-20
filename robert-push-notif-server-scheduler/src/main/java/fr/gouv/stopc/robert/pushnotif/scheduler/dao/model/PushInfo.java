package fr.gouv.stopc.robert.pushnotif.scheduler.dao.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PushInfo {

    Long id;

    String token;

    String timezone;

    String locale;

    LocalDateTime nextPlannedPush;

    LocalDateTime lastSuccessfulPush;

    LocalDateTime lastFailurePush;

    String lastErrorCode;

    int successfulPushSent;

    int failedPushSent;

    LocalDateTime creationDate;

    boolean active;

    boolean deleted;
}
