package com.dev.bank.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void rejectsCustomerAccountWithoutCustomer() {
        assertThatThrownBy(() -> Account.createCustomerAccount("EUR", null, "2000"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void newAccountsStartOpen() {
        assertThat(Account.createInternal("EUR", "1000").getAccountStatus()).isEqualTo(AccountStatus.OPEN);
    }

    @Test
    void newAccountsAreNew() {
        // Persistable: save() must INSERT directly, without an existence-check SELECT
        assertThat(Account.createInternal("EUR", "1000").isNew()).isTrue();
    }
}
