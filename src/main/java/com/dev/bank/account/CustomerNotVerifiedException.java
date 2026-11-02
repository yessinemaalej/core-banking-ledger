package com.dev.bank.account;

import com.dev.bank.customer.CustomerStatus;

import java.util.UUID;

public class CustomerNotVerifiedException extends RuntimeException {
    public CustomerNotVerifiedException(UUID customerId, CustomerStatus status) {
        super("Customer " + customerId + " is " + status + "; only VERIFIED customers can open an account");
    }
}
