package com.dev.bank.money;

import java.util.Objects;
import java.util.regex.Pattern;

public record Money(long amountMinor, String currency){
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{3}$");
    public Money{
        Objects.requireNonNull(currency, "Currency code cannot be null");

        if (!CODE_PATTERN.matcher(currency).matches()) {
            throw new IllegalArgumentException(
                "Currency code must be a 3-letter uppercase string. Received: " + currency
            );
        }
    }

    public Money plus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies must match for addition. This currency: " + this.currency + ", Other currency: " + other.currency);
        }
        return new Money(Math.addExact(this.amountMinor, other.amountMinor), this.currency);
    }
}