package com.dev.bank.customer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "customer")
public class Customer implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Override
    public UUID getId() {
        return id;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    protected Customer() {
        // for JPA
    }

    private Customer(UUID id, CustomerStatus status, Instant createdAt) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Customer create() {
        UUID id = UUID.randomUUID();
        CustomerStatus status = CustomerStatus.UNVERIFIED;
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new Customer(id, status, createdAt);
    }

}
