package com.dev.bank.outbox;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

// the Phase 4 relay will add its own claim query (FOR UPDATE SKIP LOCKED, Section 6.5)
public interface OutboxEventRepository extends Repository<OutboxEvent, UUID> {
    void save(OutboxEvent event);

    Optional<OutboxEvent> findById(UUID id);
}
