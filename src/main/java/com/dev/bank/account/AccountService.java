package com.dev.bank.account;

import com.dev.bank.audit.AuditLog;
import com.dev.bank.audit.AuditLogRepository;
import com.dev.bank.customer.Customer;
import com.dev.bank.customer.CustomerRepository;
import com.dev.bank.customer.CustomerStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;
    private final CustomerRepository customerRepository;
    public AccountService(AccountRepository accountRepository, AccountBalanceRepository accountBalanceRepository, AuditLogRepository auditLogRepository, JsonMapper jsonMapper, CustomerRepository customerRepository){
        this.accountBalanceRepository = accountBalanceRepository;
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
        this.jsonMapper = jsonMapper;
        this.customerRepository = customerRepository;
    }


    @Transactional
    public Account openInternalAccount(String currencyCode, String glAccountCode, String actor, String correlationId){
        return open(Account.createInternal(currencyCode, glAccountCode), actor, correlationId);
    }

    @Transactional
    public Account openCustomerAccount(UUID customerId , String currencyCode, String glAccountCode, String actor, String correlationId){
        // KYC gate (Section 4.2, Tier 1): checked before any write
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        if(customer.getStatus() != CustomerStatus.VERIFIED){
            throw new CustomerNotVerifiedException(customerId, customer.getStatus());
        }
        return open(Account.createCustomerAccount(currencyCode, customer.getId(), glAccountCode), actor, correlationId);
    }

    // private helper: runs inside the caller's transaction (no proxy needed, so no self-invocation trap)
    private Account open(Account account, String actor, String correlationId){
        AccountBalance accountBalance = AccountBalance.openFor(account.getId());
        String after = jsonMapper.writeValueAsString(Map.of(
                "type", account.getAccountType(),
                "status", account.getAccountStatus(),
                "currencyCode", account.getCurrencyCode(),
                "glAccountCode", account.getGlAccountCode()));

        AuditLog auditLog = AuditLog.record(actor, "ACCOUNT_OPENED", "account", account.getId().toString(), null, after, correlationId);
        accountRepository.save(account);
        accountBalanceRepository.save(accountBalance);
        auditLogRepository.save(auditLog);
        return account;
    }

}
