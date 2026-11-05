package com.dev.bank.ledger;

import com.dev.bank.account.Account;
import com.dev.bank.money.Money;

import java.util.Objects;

public record PostingLine(Account account, Money amount) {
    public PostingLine {
        Objects.requireNonNull(account, "account is required");
        Objects.requireNonNull(amount, "amount is required");
    }
}
