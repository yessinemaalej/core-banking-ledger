package com.dev.bank.money;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {
    
    @Test
    void addsTwoValidMoney(){
        Money money1 = new Money(10, "EUR");
        Money money2 = new Money(20, "EUR");
        Money result = money1.plus(money2);
        assertThat(result).isEqualTo(new Money(30, "EUR"));
    }

    @Test
    void rejectsAddingMoneyInDifferentCurrencies(){
        Money money1 = new Money(10, "EUR");
        Money money2 = new Money(20, "USD");
        assertThatThrownBy(() -> money1.plus(money2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currencies must match for addition. This currency: EUR, Other currency: USD");
    }

    @Test
    void rejectsOverFlowWhenAddingMoney(){
        Money money1 = new Money(Long.MAX_VALUE, "EUR");
        Money money2 = new Money(1, "EUR");
        assertThatThrownBy(() -> money1.plus(money2))
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("long overflow");
    }


}
