package fr.gouv.stopc.robert.pushnotif.database.model;

import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.util.Date;

@Entity
@Table(name = "PUSH", indexes = { @Index(name = "IDX_TOKEN", columnList = "token") })
@DynamicUpdate(true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@With
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
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextPlannedPush;

    @Column(name = "last_successful_push")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSuccessfulPush;

    @Column(name = "last_failure_push ")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastFailurePush;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "successful_push_sent")
    private int successfulPushSent;

    @Column(name = "failed_push_sent")
    private int failedPushSent;

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Column(name = "active")
    private boolean active;

    @Column(name = "deleted")
    private boolean deleted;
}
