package com.dev.bank.ledger;

import com.dev.bank.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.show-sql=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CurrencyRepositoryTest {

    @Autowired
    CurrencyRepository repository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void roundTrip() {
        entityManager.persist(new Currency("EUR", 2, true));
        entityManager.flush();
        entityManager.clear();

        Currency loaded = repository.findById("EUR").orElseThrow();

        assertThat(loaded.getMinorUnitExponent()).isEqualTo(2);
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCode()).isEqualTo("EUR");
    }
}
