package fr.gouv.stopc.robert.pushnotif.scheduler.dao.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PushInfo {

    private Long id;

    private String token;

    private String timezone;

    private String locale;

    private LocalDateTime nextPlannedPush;

    private LocalDateTime lastSuccessfulPush;

    private LocalDateTime lastFailurePush;

    private String lastErrorCode;

    private int successfulPushSent;

    private int failedPushSent;

    private LocalDateTime creationDate;

    private boolean active;

    private boolean deleted;
}
