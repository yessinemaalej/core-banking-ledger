package com.dev.bank.ledger;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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
}
