package com.dev.bank.account;

import com.dev.bank.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Test
    void openAccountAcceptsDebitsAndCredits() {
        Account account = accountWithStatus(AccountStatus.OPEN);

        assertThatCode(() -> account.requireCanPost(eur(1050))).doesNotThrowAnyException();
        assertThatCode(() -> account.requireCanPost(eur(-1050))).doesNotThrowAnyException();
    }

    @Test
    void debitFrozenAccountRefusesDebitsButAcceptsCredits() {
        Account account = accountWithStatus(AccountStatus.DEBIT_FROZEN);

        assertThatThrownBy(() -> account.requireCanPost(eur(1050)))
                .isInstanceOf(AccountNotPostableException.class)
                .hasMessageContaining(account.getId().toString())
                .hasMessageContaining("DEBIT_FROZEN: debits are not allowed");
        // money still arrives, e.g. a salary
        assertThatCode(() -> account.requireCanPost(eur(-1050))).doesNotThrowAnyException();
    }

    @Test
    void frozenAccountRefusesBothDirections() {
        Account account = accountWithStatus(AccountStatus.FROZEN);

        assertThatThrownBy(() -> account.requireCanPost(eur(1050))).isInstanceOf(AccountNotPostableException.class);
        assertThatThrownBy(() -> account.requireCanPost(eur(-1050)))
                .isInstanceOf(AccountNotPostableException.class)
                .hasMessageContaining("FROZEN: no postings allowed");
    }

    @Test
    void closedAccountRefusesBothDirections() {
        Account account = accountWithStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.requireCanPost(eur(1050))).isInstanceOf(AccountNotPostableException.class);
        assertThatThrownBy(() -> account.requireCanPost(eur(-1050)))
                .isInstanceOf(AccountNotPostableException.class)
                .hasMessageContaining("CLOSED: no postings allowed");
    }

    // Account has no way to change status yet (freezing and closing come later), so tests set the field directly
    static Account accountWithStatus(AccountStatus status) {
        Account account = Account.createInternal("EUR", "1000");
        ReflectionTestUtils.setField(account, "accountStatus", status);
        return account;
    }

    private static Money eur(long amountMinor) {
        return new Money(amountMinor, "EUR");
    }
}
