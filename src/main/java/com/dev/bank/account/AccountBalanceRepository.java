package com.dev.bank.account;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends Repository<AccountBalance, UUID> {
    void save(AccountBalance balance);

    Optional<AccountBalance> findById(UUID accountId);
}
