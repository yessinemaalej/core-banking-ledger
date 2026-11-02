package com.dev.bank.account;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {
    public CustomerNotFoundException(UUID customerId) {
        super("Customer " + customerId + " does not exist");
    }
}
