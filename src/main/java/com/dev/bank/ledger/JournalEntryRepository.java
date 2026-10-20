package com.dev.bank.ledger;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

// no delete: journal entries are immutable; errors are corrected by a reversal
public interface JournalEntryRepository extends Repository<JournalEntry, UUID> {
    void save(JournalEntry entry);

    Optional<JournalEntry> findById(UUID id);
}
