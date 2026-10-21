package com.dev.bank.audit;

import com.dev.bank.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.show-sql=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AuditLogRepositoryTest {

    @Autowired
    AuditLogRepository repository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void databaseAssignsTheIdOnInsert() {
        AuditLog record = AuditLog.record("sara", "CUSTOMER_CREATED", "customer", "c-1", null, "{\"status\":\"UNVERIFIED\"}", "corr-1");
        assertThat(record.getId()).isNull();

        repository.save(record);
        entityManager.flush();

        assertThat(record.getId()).isNotNull();
    }

    @Test
    void changeRecordRoundTripsWithJson() {
        AuditLog record = AuditLog.record("sara", "CUSTOMER_STATUS_CHANGED", "customer", "c-1",
                "{\"status\":\"PENDING\"}", "{\"status\":\"VERIFIED\"}", "corr-2");
        repository.save(record);
        entityManager.flush();
        entityManager.clear();

        AuditLog loaded = repository.findById(record.getId()).orElseThrow();

        assertThat(loaded.getActor()).isEqualTo("sara");
        assertThat(loaded.getAction()).isEqualTo("CUSTOMER_STATUS_CHANGED");
        assertThat(loaded.getObjectType()).isEqualTo("customer");
        assertThat(loaded.getObjectId()).isEqualTo("c-1");
        // Postgres normalizes JSONB (spacing, key order), so compare meaning, not exact text
        assertThat(loaded.getBefore()).contains("\"status\"").contains("PENDING");
        assertThat(loaded.getAfter()).contains("\"status\"").contains("VERIFIED");
        assertThat(loaded.getCorrelationId()).isEqualTo("corr-2");
        assertThat(loaded.getOccurredAt()).isEqualTo(record.getOccurredAt());
        assertThat(loaded.getPrevHash()).isNull();
        assertThat(loaded.getHash()).isNull();
    }

    @Test
    void idsIncreaseInInsertOrder() {
        AuditLog first = AuditLog.record("sara", "A", "customer", "c-1", null, null, null);
        AuditLog second = AuditLog.record("sara", "B", "customer", "c-1", null, null, null);
        repository.save(first);
        repository.save(second);
        entityManager.flush();

        assertThat(second.getId()).isGreaterThan(first.getId());
    }

    @Test
    void rejectsInvalidJson() {
        // JSONB is checked by Postgres: malformed JSON can't be stored.
        // With IDENTITY, Hibernate must INSERT at save() to learn the ID, so the error surfaces there, not at flush()
        AuditLog malformed = AuditLog.record("sara", "X", "customer", "c-1", null, "{not json", null);

        assertThatThrownBy(() -> {
            repository.save(malformed);
            entityManager.flush();
        }).hasMessageContaining("json");
    }

    @Test
    void rejectsRecordWithoutActor() {
        assertThatThrownBy(() -> AuditLog.record(null, "X", "customer", "c-1", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
