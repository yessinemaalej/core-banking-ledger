package com.dev.bank.ledger;

// the entry breaks double entry: fewer than two postings, or a currency whose postings don't sum to zero
public class UnbalancedJournalEntryException extends RuntimeException {
    public UnbalancedJournalEntryException(String message) {
        super(message);
    }
}
