package com.dev.bank.ledger;

import com.dev.bank.account.Account;
import com.dev.bank.money.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private final Account eurAccount = Account.createInternal("EUR", "1000");
    private final JournalEntry entry = JournalEntry.transfer(TODAY, TODAY.minusDays(1), "alice", null, null);

    @Test
    void copiesAccountEntryAndValueDate() {
        Posting posting = Posting.of(entry, eurAccount, new Money(-1050, "EUR"));

        assertThat(posting.getAccountId()).isEqualTo(eurAccount.getId());
        assertThat(posting.getJournalEntryId()).isEqualTo(entry.getId());
        assertThat(posting.getCurrencyCode()).isEqualTo("EUR");
        assertThat(posting.getAmountMinor()).isEqualTo(-1050);
        assertThat(posting.getValueDate()).isEqualTo(TODAY.minusDays(1));
        assertThat(posting.isNew()).isTrue();
    }

    @Test
    void rejectsAmountInAnotherCurrency() {
        assertThatThrownBy(() -> Posting.of(entry, eurAccount, new Money(100, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    @Test
    void rejectsZeroAmount() {
        assertThatThrownBy(() -> Posting.of(entry, eurAccount, new Money(0, "EUR")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingJournalEntry() {
        assertThatThrownBy(() -> Posting.of(null, eurAccount, new Money(100, "EUR")))
                .isInstanceOf(NullPointerException.class);
    }
}
