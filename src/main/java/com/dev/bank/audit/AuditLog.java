package com.dev.bank.audit;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "audit_log")
@Getter
public class AuditLog {
    // database-generated: the Phase 4 hash chain needs a strict order; null until INSERT, so no Persistable
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "actor")
    private String actor;

    @Column(name = "action")
    private String action;

    @Column(name = "object_type")
    private String objectType;

    @Column(name = "object_id")
    private String objectId;

    // null when the object was created
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before")
    private String before;

    // null when the object was removed
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after")
    private String after;

    @Column(name = "correlation_id")
    private String correlationId;

    // filled in Phase 4 (M4.3)
    @Column(name="prev_hash")
    private byte[] prevHash;

    @Column(name="hash")
    private byte[] hash;

    protected AuditLog(){}

    private AuditLog(Instant occurredAt, String actor, String action, String objectType, String objectId, String before, String after, String correlationId){
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.action = action;
        this.objectId = objectId;
        this.objectType = objectType;
        this.before = before;
        this.after = after;
        this.correlationId = correlationId;
    }

    public static AuditLog record(String actor, String action, String objectType, String objectId, String before, String after, String correlationId){
        Objects.requireNonNull(actor, "actor is required for an audit record");
        Objects.requireNonNull(action, "action is required for an audit record");
        Objects.requireNonNull(objectType, "objectType is required for an audit record");
        Objects.requireNonNull(objectId, "objectId is required for an audit record");

        return new AuditLog(Instant.now().truncatedTo(ChronoUnit.MICROS), actor, action, objectType, objectId, before, after, correlationId);
    }
}
