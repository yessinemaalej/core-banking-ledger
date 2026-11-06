package com.dev.bank.ledger;

import com.dev.bank.money.Money;

import java.util.UUID;

public record TransferLine(UUID accountId, Money amount) {}
