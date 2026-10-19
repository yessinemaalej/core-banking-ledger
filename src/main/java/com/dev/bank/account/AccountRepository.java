package com.dev.bank.account;

import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends Repository<Account, UUID> {
    void save(Account account);
    Optional<Account> findById(UUID id);
    
}
