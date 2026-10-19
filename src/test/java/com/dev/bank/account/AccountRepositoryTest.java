package com.dev.bank.account;

import com.dev.bank.TestcontainersConfiguration;
import com.dev.bank.customer.Customer;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.show-sql=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AccountRepositoryTest {

    @Autowired
    AccountRepository repository;
    @Autowired
    TestEntityManager entityManager;

    // fields, not locals: the tests need these after @BeforeEach has run
    private Customer customer;

    @BeforeEach
    void insertReferenceData() {
        entityManager.persist(new Currency("EUR", 2, true));
        entityManager.persist(new GlAccount("1000", "Cash", AccountClass.ASSET, NormalSide.DEBIT));
        entityManager.persist(new GlAccount("2000", "Customer deposits", AccountClass.LIABILITY, NormalSide.CREDIT));
        customer = Customer.create();
        entityManager.persist(customer);
        entityManager.flush();
    }

    @Test
    void internalAccountRoundTrips() {
        Account account = Account.createInternal("EUR", "1000");
        repository.save(account);
        entityManager.flush();
        entityManager.clear();

        Account loaded = repository.findById(account.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(account.getId());
        assertThat(loaded.getCustomerId()).isNull();
        assertThat(loaded.getAccountType()).isEqualTo(AccountType.INTERNAL);
        assertThat(loaded.getAccountStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(loaded.getCurrencyCode()).isEqualTo("EUR");
        assertThat(loaded.getGlAccountCode()).isEqualTo("1000");
        assertThat(loaded.getCreatedAt()).isEqualTo(account.getCreatedAt());
    }

    @Test
    void customerAccountRoundTrips() {
        Account account = Account.createCustomerAccount("EUR", customer.getId(), "2000");
        repository.save(account);
        entityManager.flush();
        entityManager.clear();

        Account loaded = repository.findById(account.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(account.getId());
        assertThat(loaded.getCustomerId()).isEqualTo(customer.getId());
        assertThat(loaded.getAccountType()).isEqualTo(AccountType.CUSTOMER_DEPOSIT);
        assertThat(loaded.getAccountStatus()).isEqualTo(AccountStatus.OPEN);
        assertThat(loaded.getCurrencyCode()).isEqualTo("EUR");
        assertThat(loaded.getGlAccountCode()).isEqualTo("2000");
        assertThat(loaded.getCreatedAt()).isEqualTo(account.getCreatedAt());
    }

    @Test
    void rejectsUnknownCurrency() {
        // XYZ was never inserted: the foreign key to currency must reject it at flush
        repository.save(Account.createInternal("XYZ", "1000"));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("account_currency_code_fkey");
    }
}
