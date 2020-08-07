package fr.gouv.stopc.robert.pushnotif.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "push_info")
@DynamicUpdate(true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PushInfo {

    @Id
    private String token;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "locale")
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

    @CreatedDate
    @Column(name = "creation_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;
 
    @Column(name = "active")
    private boolean active;

    @Column(name = "deleted")
    private boolean deleted;
}
