package com.dev.bank.glaccount;

import java.util.Optional;

import org.springframework.data.repository.Repository;

public interface GlAccountRepository extends Repository<GlAccount, String> {
    Optional<GlAccount> findById(String id);
}
