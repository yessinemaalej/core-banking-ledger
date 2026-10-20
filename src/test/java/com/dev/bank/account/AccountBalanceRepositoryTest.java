package com.dev.bank.account;

import com.dev.bank.TestcontainersConfiguration;
import com.dev.bank.glaccount.AccountClass;
import com.dev.bank.glaccount.GlAccount;
import com.dev.bank.glaccount.NormalSide;
import com.dev.bank.ledger.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.show-sql=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AccountBalanceRepositoryTest {

    @Autowired
    AccountBalanceRepository repository;
    @Autowired
    TestEntityManager entityManager;

    private Account account;

    @BeforeEach
    void insertAccount() {
        entityManager.persist(new Currency("EUR", 2, true));
        entityManager.persist(new GlAccount("1000", "Cash", AccountClass.ASSET, NormalSide.DEBIT));
        account = Account.createInternal("EUR", "1000");
        entityManager.persist(account);
        entityManager.flush();
    }

    @Test
    void newBalanceRoundTripsAtZero() {
        AccountBalance balance = AccountBalance.openFor(account.getId());
        repository.save(balance);
        entityManager.flush();
        entityManager.clear();

        AccountBalance loaded = repository.findById(account.getId()).orElseThrow();

        assertThat(loaded.getAccountId()).isEqualTo(account.getId());
        assertThat(loaded.getBalanceMinor()).isZero();
        assertThat(loaded.getVersion()).isZero();
        assertThat(loaded.getUpdatedAt()).isEqualTo(balance.getUpdatedAt());
    }

    @Test
    void rejectsBalanceForUnknownAccount() {
        repository.save(AccountBalance.openFor(UUID.randomUUID()));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("account_balance_account_id_fkey");
    }

    @Test
    void rejectsSecondBalanceForSameAccount() {
        // one-to-one: the primary key allows exactly one balance row per account
        entityManager.persist(AccountBalance.openFor(account.getId()));
        entityManager.flush();
        entityManager.clear();

        repository.save(AccountBalance.openFor(account.getId()));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("account_balance_pkey");
    }
}
