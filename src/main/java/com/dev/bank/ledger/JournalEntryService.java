package com.dev.bank.ledger;

import com.dev.bank.account.*;
import com.dev.bank.audit.AuditLog;
import com.dev.bank.audit.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.*;

@Service
public class JournalEntryService {
    private final AccountBalanceRepository accountBalanceRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PostingRepository postingRepository;
    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;


    public JournalEntryService(AccountBalanceRepository accountBalanceRepository, AccountRepository accountRepository, JournalEntryRepository journalEntryRepository, PostingRepository postingRepository, AuditLogRepository auditLogRepository, JsonMapper jsonMapper){
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.postingRepository = postingRepository;
        this.auditLogRepository = auditLogRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public PostedEntry postTransfer(LocalDate businessDate, LocalDate valueDate, List<TransferLine> lines,
                                    String actor, String correlationId, String idempotencyKey) {
        // lock in ascending ID order, each account once: concurrent transfers can't deadlock
        List<UUID> accountIds = lines.stream().map(TransferLine::accountId).distinct().sorted().toList();
        Map<UUID, Account> accountsById = new HashMap<>();
        Map<UUID, AccountBalance> balancesById = new HashMap<>();

        for(UUID accountId: accountIds){
            AccountBalance accountBalance = accountBalanceRepository.findForUpdateById(accountId).orElseThrow(()->new AccountNotFoundException(accountId));
            Account account = accountRepository.findById(accountId).orElseThrow(()->new AccountNotFoundException(accountId));
            accountsById.put(accountId, account);
            balancesById.put(accountId, accountBalance);
        }

        // one posting per request line (an account may appear on several lines)
        List<PostingLine> postingLines = new ArrayList<>();
        for (TransferLine line : lines) {
            Account account = accountsById.get(line.accountId());
            postingLines.add(new PostingLine(account, line.amount()));
        }

        // balance, currency, non-zero and account status rules all run here
        PostedEntry postedEntry = JournalEntry.post(businessDate,valueDate,actor, idempotencyKey, correlationId, postingLines);
        journalEntryRepository.save(postedEntry.entry());

        List<Posting> postings = postedEntry.postings();
        for(Posting posting: postings){
            postingRepository.save(posting);
        }
        // balances are locked and managed: dirty checking writes the UPDATE at commit
        for(Posting posting: postings){
            long amountMinor = posting.getAmountMinor();
            AccountBalance accountBalance = balancesById.get(posting.getAccountId());
            accountBalance.apply(amountMinor);
        }
        List<Map<String, Object>> postingsJson = new ArrayList<>();
        for (Posting posting : postings) {
            postingsJson.add(Map.of(
                    "accountId", posting.getAccountId(),
                    "amountMinor", posting.getAmountMinor(),
                    "currencyCode", posting.getCurrencyCode()));
        }

        String after = jsonMapper.writeValueAsString(Map.of(
                "type", postedEntry.entry().getType(),
                "businessDate", postedEntry.entry().getBusinessDate(),
                "valueDate", postedEntry.entry().getValueDate(),
                "postings", postingsJson));

        AuditLog audit = AuditLog.record(actor, "JOURNAL_ENTRY_POSTED", "journal_entry", postedEntry.entry().getId().toString(), null, after, correlationId);
        auditLogRepository.save(audit);
        return postedEntry;
    }
}
