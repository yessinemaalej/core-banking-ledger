package com.dev.bank.glaccount;

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
class GlAccountRepositoryTest {

    @Autowired
    GlAccountRepository repository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void roundTrip() {
        GlAccount glAccount = new GlAccount("1000", "Cash", AccountClass.ASSET, NormalSide.DEBIT);
        entityManager.persist(glAccount);
        entityManager.flush();
        entityManager.clear();

        GlAccount loaded = repository.findById("1000").orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Cash");
        assertThat(loaded.getAccountClass()).isEqualTo(AccountClass.ASSET);
        assertThat(loaded.getNormalSide()).isEqualTo(NormalSide.DEBIT);
        assertThat(loaded.getCode()).isEqualTo("1000");
    }
}
