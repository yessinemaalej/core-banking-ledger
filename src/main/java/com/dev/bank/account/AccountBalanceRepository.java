package com.dev.bank.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends Repository<AccountBalance, UUID> {
    void save(AccountBalance balance);

    Optional<AccountBalance> findById(UUID accountId);

    // SELECT ... FOR UPDATE: a concurrent transaction waits here until this one commits (no lost update).
    // Only meaningful inside a transaction; callers must lock accounts in ascending ID order to avoid deadlocks.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountBalance> findForUpdateById(UUID accountId);
}
