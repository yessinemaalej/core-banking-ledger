package com.dev.bank.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Cached balance of one account: the signed sum of its postings (debit +, credit -).
 * A customer deposit is a liability, so the customer-facing balance is {@code -balanceMinor}.
 */
@Entity
@Table(name = "account_balance")
public class AccountBalance implements Persistable<UUID> {

    // same value as account.id: one-to-one, the foreign key is the primary key
    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "balance_minor")
    private long balanceMinor;

    // plain column for now; whether it becomes @Version (optimistic locking) is the M2.4 decision
    @Column(name = "version")
    private long version;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    protected AccountBalance() {
        // for JPA
    }

    private AccountBalance(UUID accountId, long balanceMinor, long version, Instant updatedAt) {
        this.accountId = accountId;
        this.balanceMinor = balanceMinor;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    /** Every account starts with exactly one balance row, at zero. */
    public static AccountBalance openFor(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId is required");
        return new AccountBalance(accountId, 0, 0, Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    /**
     * Applies one posting's signed amount. Call only on a balance loaded with a lock
     * ({@link AccountBalanceRepository#findForUpdateById}), or concurrent postings can overwrite each other.
     * The change is written by dirty checking at commit; no save() needed.
     */
    public void apply(long amountMinor) {
        this.balanceMinor = Math.addExact(balanceMinor, amountMinor);
        this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public UUID getId() {
        return accountId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public long getBalanceMinor() {
        return balanceMinor;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
