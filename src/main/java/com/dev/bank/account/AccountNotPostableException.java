package com.dev.bank.account;

public class AccountNotPostableException extends RuntimeException {
    public AccountNotPostableException(String message) {
        super(message);
    }
}
