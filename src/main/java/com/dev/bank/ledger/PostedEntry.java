package com.dev.bank.ledger;

import java.util.List;

public record PostedEntry(JournalEntry entry, List<Posting> postings) {}

