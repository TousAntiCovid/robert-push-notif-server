package fr.gouv.stopc.robert.pushnotif.server.ws.model;

import fr.gouv.stopc.robert.pushnotif.server.ws.repository.TimeStampInstantAttributeConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "PUSH", indexes = { @Index(name = "IDX_TOKEN", columnList = "token") })
@DynamicUpdate
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PushInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "token", unique = true, nullable = false)
    private String token;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "locale", nullable = false)
    private String locale;

    @Column(name = "next_planned_push")
    @Convert(converter = TimeStampInstantAttributeConverter.class)
    private Instant nextPlannedPush;

    @Column(name = "last_successful_push")
    @Convert(converter = TimeStampInstantAttributeConverter.class)
    private Instant lastSuccessfulPush;

    @Column(name = "last_failure_push ")
    @Convert(converter = TimeStampInstantAttributeConverter.class)
    private Instant lastFailurePush;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "successful_push_sent")
    private int successfulPushSent;

    @Column(name = "failed_push_sent")
    private int failedPushSent;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    private Instant creationDate;

    @Column(name = "active")
    private boolean active;

    @Column(name = "deleted")
    private boolean deleted;
}
