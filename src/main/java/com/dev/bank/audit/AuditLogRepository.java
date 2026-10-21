package com.dev.bank.audit;

import org.springframework.data.repository.Repository;

import java.util.Optional;

// append-only (Section 6.5): no update, no delete
public interface AuditLogRepository extends Repository<AuditLog, Long> {
    AuditLog save(AuditLog record);

    Optional<AuditLog> findById(Long id);
}
