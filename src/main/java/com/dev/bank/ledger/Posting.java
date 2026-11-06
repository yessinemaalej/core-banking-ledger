package com.dev.bank.ledger;

import com.dev.bank.account.Account;
import com.dev.bank.money.Money;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="posting")
@Getter
public class Posting  implements Persistable<UUID> {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name="journal_entry_id")
    private UUID journalEntryId;

    @Column(name="account_id")
    private UUID accountId;

    @Column(name="currency_code")
    private String currencyCode;

    @Column(name="amount_minor")
    private long amountMinor;

    @Column(name="value_date")
    private LocalDate valueDate;

    @Column(name="created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
    protected Posting(){}

    private Posting(UUID id, UUID journalEntryId, UUID accountId, String currencyCode, long amountMinor, LocalDate valueDate, Instant createdAt){
        this.id = id;
        this.journalEntryId = journalEntryId;
        this.accountId = accountId;
        this.currencyCode = currencyCode;
        this.amountMinor = amountMinor;
        this.valueDate = valueDate;
        this.createdAt = createdAt;

    }

    // package-private: only JournalEntry.post(...) builds postings, after checking the balance
    static Posting of(JournalEntry journalEntry, Account account, Money amount){

        Objects.requireNonNull(account, "account cannot be null");
        Objects.requireNonNull(journalEntry, "journalEntry cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");


        if(!Objects.equals(amount.currency(), account.getCurrencyCode())){
            throw new IllegalArgumentException(
                    "Posting currency " + amount.currency() + " does not match account currency " + account.getCurrencyCode());
        }
        if(amount.amountMinor() ==0){
            throw new IllegalArgumentException("A posting must move money: amount cannot be zero");
        }
        account.requireCanPost(amount);
        UUID id = UUID.randomUUID();
        return new Posting(id,journalEntry.getId(), account.getId(), amount.currency(), amount.amountMinor(), journalEntry.getValueDate(), Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

}
