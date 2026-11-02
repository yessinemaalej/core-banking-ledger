package com.dev.bank.account;

import java.util.UUID;

public record AccountResponseDto(
     UUID id,
     AccountType type,
     AccountStatus status,
     String currencyCode,
     String glAccountCode,
     UUID customerId
){
    static AccountResponseDto from(Account account){
        return new AccountResponseDto(account.getId(), account.getAccountType(), account.getAccountStatus(),
                account.getCurrencyCode(), account.getGlAccountCode(), account.getCustomerId());
    }
}
