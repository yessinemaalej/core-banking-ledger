package com.dev.bank.ledger;

import com.dev.bank.TestcontainersConfiguration;
import com.dev.bank.account.Account;
import com.dev.bank.glaccount.AccountClass;
import com.dev.bank.glaccount.GlAccount;
import com.dev.bank.glaccount.NormalSide;
import com.dev.bank.money.Money;
import org.junit.jupiter.api.BeforeEach;
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
class PostingRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Autowired
    PostingRepository repository;
    @Autowired
    TestEntityManager entityManager;

    private Account cash;
    private JournalEntry entry;

    @BeforeEach
    void insertAccountAndEntry() {
        entityManager.persist(new Currency("EUR", 2, true));
        entityManager.persist(new GlAccount("1000", "Cash", AccountClass.ASSET, NormalSide.DEBIT));
        cash = Account.createInternal("EUR", "1000");
        entityManager.persist(cash);
        entry = JournalEntry.transfer(TODAY, TODAY, "system", null, null);
        entityManager.persist(entry);
        entityManager.flush();
    }

    @Test
    void postingRoundTrips() {
        Posting posting = Posting.of(entry, cash, new Money(-1050, "EUR"));
        repository.save(posting);
        entityManager.flush();
        entityManager.clear();

        Posting loaded = repository.findById(posting.getId()).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(posting.getId());
        assertThat(loaded.getJournalEntryId()).isEqualTo(entry.getId());
        assertThat(loaded.getAccountId()).isEqualTo(cash.getId());
        assertThat(loaded.getCurrencyCode()).isEqualTo("EUR");
        assertThat(loaded.getAmountMinor()).isEqualTo(-1050);
        assertThat(loaded.getValueDate()).isEqualTo(TODAY);
        assertThat(loaded.getCreatedAt()).isEqualTo(posting.getCreatedAt());
    }

    @Test
    void rejectsPostingOnUnknownAccount() {
        // the account object exists in Java but was never saved: the composite foreign key must reject it
        Account unsaved = Account.createInternal("EUR", "1000");
        repository.save(Posting.of(entry, unsaved, new Money(100, "EUR")));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("posting_account_id_currency_code_fkey");
    }

    @Test
    void rejectsPostingOnUnknownJournalEntry() {
        JournalEntry unsaved = JournalEntry.transfer(TODAY, TODAY, "system", null, null);
        repository.save(Posting.of(unsaved, cash, new Money(100, "EUR")));

        assertThatThrownBy(() -> entityManager.flush())
                .hasMessageContaining("posting_journal_entry_id_fkey");
    }
}
