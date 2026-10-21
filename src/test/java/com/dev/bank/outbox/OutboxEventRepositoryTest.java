package com.dev.bank.outbox;

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
class OutboxEventRepositoryTest {

    private static final String PAYLOAD = "{\"amountMinor\":3000,\"currency\":\"EUR\"}";

    @Autowired
    OutboxEventRepository repository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void eventRoundTripsUnpublished() {
        OutboxEvent event = OutboxEvent.of("account", "acc-1", 1, "TransferPosted", PAYLOAD);
        repository.save(event);
        entityManager.flush();
        entityManager.clear();

        OutboxEvent loaded = repository.findById(event.getId()).orElseThrow();

        assertThat(loaded.getAggregateType()).isEqualTo("account");
        assertThat(loaded.getAggregateId()).isEqualTo("acc-1");
        assertThat(loaded.getAggregateSeq()).isEqualTo(1);
        assertThat(loaded.getEventType()).isEqualTo("TransferPosted");
        assertThat(loaded.getPayload()).contains("\"amountMinor\"").contains("3000");
        assertThat(loaded.getCreatedAt()).isEqualTo(event.getCreatedAt());
        assertThat(loaded.getPublishedAt()).isNull();
    }

    @Test
    void rejectsSameSequenceNumberTwiceForOneAggregate() {
        repository.save(OutboxEvent.of("account", "acc-1", 1, "TransferPosted", PAYLOAD));
        entityManager.flush();

        repository.save(OutboxEvent.of("account", "acc-1", 1, "TransferPosted", PAYLOAD));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("outbox_event_aggregate_type_aggregate_id_aggregate_seq_key");
    }

    @Test
    void allowsSameSequenceNumberForDifferentAggregates() {
        repository.save(OutboxEvent.of("account", "acc-1", 1, "TransferPosted", PAYLOAD));
        repository.save(OutboxEvent.of("account", "acc-2", 1, "TransferPosted", PAYLOAD));

        entityManager.flush();
    }

    @Test
    void rejectsSequenceBelowOne() {
        assertThatThrownBy(() -> OutboxEvent.of("account", "acc-1", 0, "TransferPosted", PAYLOAD))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
