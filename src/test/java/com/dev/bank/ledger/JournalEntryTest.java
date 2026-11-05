package com.dev.bank.ledger;

import com.dev.bank.money.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Test
    void transferIsNeverAReversal() {
        JournalEntry entry = JournalEntry.transfer(TODAY, TODAY, "alice", null, null);

        assertThat(entry.getType()).isEqualTo(JournalEntryType.TRANSFER);
        assertThat(entry.getReversesEntryId()).isNull();
        assertThat(entry.isNew()).isTrue();
    }

    @Test
    void reversalPointsAtOriginalAndKeepsItsValueDate() {
        JournalEntry original = JournalEntry.transfer(TODAY, TODAY.minusDays(3), "alice", null, null);

        JournalEntry reversal = JournalEntry.reversalOf(original, "sara", TODAY, null, null);

        assertThat(reversal.getType()).isEqualTo(JournalEntryType.REVERSAL);
        assertThat(reversal.getReversesEntryId()).isEqualTo(original.getId());
        assertThat(reversal.getValueDate()).isEqualTo(TODAY.minusDays(3));
        assertThat(reversal.getCreatedBy()).isEqualTo("sara");
    }

    @Test
    void rejectsReversalWithoutOriginal() {
        assertThatThrownBy(() -> JournalEntry.reversalOf(null, "sara", TODAY, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsReversalWithoutActor() {
        JournalEntry original = JournalEntry.transfer(TODAY, TODAY, "alice", null, null);

        assertThatThrownBy(() -> JournalEntry.reversalOf(original, null, TODAY, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void balancedEntryIsAcceptedEvenWhenRunningTotalIsNotZero() {
        // after the first two amounts the EUR total is 550: only the final total may be checked
        assertThatCode(() -> JournalEntry.requireBalanced(List.of(eur(1050), eur(-500), eur(-550))))
                .doesNotThrowAnyException();
    }

    @Test
    void eachCurrencyBalancedOnItsOwnIsAccepted() {
        assertThatCode(() -> JournalEntry.requireBalanced(List.of(eur(1050), usd(2000), eur(-1050), usd(-2000))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnbalancedCurrencyAndNamesIt() {
        assertThatThrownBy(() -> JournalEntry.requireBalanced(List.of(eur(1050), eur(-1000))))
                .isInstanceOf(UnbalancedJournalEntryException.class)
                .hasMessageContaining("EUR postings sum to 50");
    }

    @Test
    void amountsInDifferentCurrenciesDoNotCancelOut() {
        // 1050 + (-1050) is zero overall, but EUR and USD each sum to non-zero
        assertThatThrownBy(() -> JournalEntry.requireBalanced(List.of(eur(1050), usd(-1050))))
                .isInstanceOf(UnbalancedJournalEntryException.class);
    }

    @Test
    void rejectsEmptyEntry() {
        // with no postings, "every currency sums to zero" is vacuously true
        assertThatThrownBy(() -> JournalEntry.requireBalanced(List.of()))
                .isInstanceOf(UnbalancedJournalEntryException.class)
                .hasMessageContaining("at least 2 postings");
    }

    @Test
    void rejectsSinglePosting() {
        assertThatThrownBy(() -> JournalEntry.requireBalanced(List.of(eur(1050))))
                .isInstanceOf(UnbalancedJournalEntryException.class);
    }

    @Test
    void overflowWhileSummingThrowsInsteadOfWrapping() {
        assertThatThrownBy(() -> JournalEntry.requireBalanced(List.of(eur(Long.MAX_VALUE), eur(1), eur(-1))))
                .isInstanceOf(ArithmeticException.class);
    }

    private static Money eur(long amountMinor) {
        return new Money(amountMinor, "EUR");
    }

    private static Money usd(long amountMinor) {
        return new Money(amountMinor, "USD");
    }
}
