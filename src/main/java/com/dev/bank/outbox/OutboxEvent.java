package com.dev.bank.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * An event written in the same transaction as the state change it describes (transactional outbox, Section 6.5).
 * A relay publishes it to Kafka later (Phase 4) and sets {@code publishedAt}.
 */
@Entity
@Table(name = "outbox_event")
public class OutboxEvent implements Persistable<UUID> {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "aggregate_type")
    private String aggregateType;

    // the Kafka key: all events of one account or loan land in one partition, in order
    @Column(name = "aggregate_id")
    private String aggregateId;

    // 1, 2, 3… per aggregate, so consumers can detect a missing event
    @Column(name = "aggregate_seq")
    private long aggregateSeq;

    @Column(name = "event_type")
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Column(name = "created_at")
    private Instant createdAt;

    // null until the Phase 4 relay has published it
    @Column(name = "published_at")
    private Instant publishedAt;

    @Transient
    private boolean isNew = true;

    protected OutboxEvent() {
        // for JPA
    }

    private OutboxEvent(UUID id, String aggregateType, String aggregateId, long aggregateSeq,
                        String eventType, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.aggregateSeq = aggregateSeq;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public static OutboxEvent of(String aggregateType, String aggregateId, long aggregateSeq,
                                 String eventType, String payload) {
        Objects.requireNonNull(aggregateType, "aggregateType is required");
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(payload, "payload is required");
        if (aggregateSeq < 1) {
            throw new IllegalArgumentException("aggregateSeq starts at 1, got " + aggregateSeq);
        }
        return new OutboxEvent(UUID.randomUUID(), aggregateType, aggregateId, aggregateSeq,
                eventType, payload, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public long getAggregateSeq() {
        return aggregateSeq;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
