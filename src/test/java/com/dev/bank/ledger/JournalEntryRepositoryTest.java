package com.dev.bank.ledger;

import com.dev.bank.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.show-sql=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JournalEntryRepositoryTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 19);
    private static final LocalDate VALUE_DATE = LocalDate.of(2026, 10, 18);

    @Autowired
    JournalEntryRepository repository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void transferRoundTrips() {
        JournalEntry entry = JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", "idem-1", "corr-1");
        repository.save(entry);
        entityManager.flush();
        entityManager.clear();

        JournalEntry loaded = repository.findById(entry.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(entry.getId());
        assertThat(loaded.getType()).isEqualTo(JournalEntryType.TRANSFER);
        assertThat(loaded.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(loaded.getBusinessDate()).isEqualTo(BUSINESS_DATE);
        assertThat(loaded.getValueDate()).isEqualTo(VALUE_DATE);
        assertThat(loaded.getCreatedBy()).isEqualTo("alice");
        assertThat(loaded.getIdempotencyKey()).isEqualTo("idem-1");
        assertThat(loaded.getCorrelationId()).isEqualTo("corr-1");
        assertThat(loaded.getReversesEntryId()).isNull();
        assertThat(loaded.getCreatedAt()).isEqualTo(entry.getCreatedAt());
    }

    @Test
    void reversalRoundTrips() {
        JournalEntry original = JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", null, "corr-1");
        repository.save(original);
        JournalEntry reversal = JournalEntry.reversalOf(original, "sara", BUSINESS_DATE.plusDays(1), "corr-2", null);
        repository.save(reversal);
        entityManager.flush();
        entityManager.clear();

        JournalEntry loaded = repository.findById(reversal.getId()).orElseThrow();

        assertThat(loaded.getType()).isEqualTo(JournalEntryType.REVERSAL);
        assertThat(loaded.getReversesEntryId()).isEqualTo(original.getId());
        assertThat(loaded.getCreatedBy()).isEqualTo("sara");
        assertThat(loaded.getValueDate()).isEqualTo(VALUE_DATE);
        assertThat(loaded.getBusinessDate()).isEqualTo(BUSINESS_DATE.plusDays(1));
    }

    @Test
    void rejectsSecondReversalOfSameEntry() {
        JournalEntry original = JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", null, null);
        repository.save(original);
        repository.save(JournalEntry.reversalOf(original, "sara", BUSINESS_DATE, null, null));
        entityManager.flush();

        repository.save(JournalEntry.reversalOf(original, "sara", BUSINESS_DATE, null, null));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("journal_entry_reverses_entry_id_key");
    }

    @Test
    void rejectsReusedIdempotencyKey() {
        repository.save(JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", "idem-1", null));
        entityManager.flush();

        repository.save(JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", "idem-1", null));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("journal_entry_idempotency_key_key");
    }

    @Test
    void allowsManyEntriesWithoutIdempotencyKey() {
        // until Phase 2 the key is optional; Postgres allows many NULLs in a UNIQUE column
        repository.save(JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", null, null));
        repository.save(JournalEntry.transfer(BUSINESS_DATE, VALUE_DATE, "alice", null, null));

        entityManager.flush();
    }
}
