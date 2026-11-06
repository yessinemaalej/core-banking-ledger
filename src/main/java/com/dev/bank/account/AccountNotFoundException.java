package com.dev.bank.account;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(UUID accountId) {
        super("Account " + accountId + " does not exist");
    }
}
