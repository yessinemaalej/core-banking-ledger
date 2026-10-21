package com.dev.bank.account;

import com.dev.bank.audit.AuditLog;
import com.dev.bank.audit.AuditLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;
    public AccountService(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository, AuditLogRepository auditLogRepository, JsonMapper jsonMapper){
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
        this.jsonMapper = jsonMapper;
    }


    @Transactional
    public Account openInternalAccount(String currencyCode, String glAccountCode, String actor, String correlationId){
        Account internalAccount = Account.createInternal(currencyCode, glAccountCode);
        AccountBalance accountBalance = AccountBalance.openFor(internalAccount.getId());
        String after = jsonMapper.writeValueAsString(Map.of(
                "type", internalAccount.getAccountType(),
                "status", internalAccount.getAccountStatus(),
                "currencyCode", internalAccount.getCurrencyCode(),
                "glAccountCode", internalAccount.getGlAccountCode()));

        AuditLog auditLog = AuditLog.record(actor, "ACCOUNT_OPENED", "account", internalAccount.getId().toString(), null, after, correlationId);
        accountRepository.save(internalAccount);
        accountBalanceRepository.save(accountBalance);
        auditLogRepository.save(auditLog);
        return internalAccount;
    }

}
