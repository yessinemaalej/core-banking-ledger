package com.dev.bank.account;

import com.dev.bank.TestcontainersConfiguration;
import com.dev.bank.audit.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Proves the service's own transaction: no @Transactional on these tests, so the service really commits or
 * really rolls back, and each test checks the database afterwards like a second user would.
 * Commits are real, so every test cleans up in @AfterEach.
 */
class AccountServiceTest {

    @SpringBootTest
    @Import(TestcontainersConfiguration.class)
    abstract static class WithDatabase {

        @Autowired
        AccountService accountService;
        @Autowired
        JdbcTemplate jdbc;

        @BeforeEach
        void insertReferenceData() {
            // reference data has read-only repositories, so insert it with plain SQL
            jdbc.update("INSERT INTO currency VALUES ('EUR', 2, true)");
            jdbc.update("INSERT INTO gl_account VALUES ('1000', 'Cash', 'ASSET', 'DEBIT')");
        }

        @AfterEach
        void cleanUp() {
            jdbc.execute("TRUNCATE audit_log, account_balance, account, gl_account, currency CASCADE");
        }

        long count(String sql, Object... args) {
            return jdbc.queryForObject(sql, Long.class, args);
        }
    }

    @Nested
    class WhenEverythingSucceeds extends WithDatabase {

        @Test
        void commitsAccountBalanceAndAudit() {
            Account account = accountService.openInternalAccount("EUR", "1000", "sara", "corr-1");

            String id = account.getId().toString();
            assertThat(count("SELECT count(*) FROM account WHERE id = ?::uuid", id)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT balance_minor FROM account_balance WHERE account_id = ?::uuid", Long.class, id)).isZero();
            assertThat(count("SELECT count(*) FROM audit_log WHERE action = 'ACCOUNT_OPENED' AND object_id = ?"
                    + " AND actor = 'sara' AND correlation_id = 'corr-1' AND before IS NULL"
                    + " AND after ->> 'currencyCode' = 'EUR'", id)).isEqualTo(1);
        }

        @Test
        void unknownCurrencyIsRejectedAndLeavesNothingBehind() {
            assertThatThrownBy(() -> accountService.openInternalAccount("XYZ", "1000", "sara", "corr-2"))
                    .hasStackTraceContaining("account_currency_code_fkey");

            assertThat(count("SELECT count(*) FROM account")).isZero();
            assertThat(count("SELECT count(*) FROM audit_log")).isZero();
        }
    }

    @Nested
    class WhenTheAuditWriteFails extends WithDatabase {

        // replaces the real repository in this context only: every audit save throws
        @MockitoBean
        AuditLogRepository auditLogRepository;

        @Test
        void accountAndBalanceAreRolledBack() {
            when(auditLogRepository.save(any())).thenThrow(new IllegalStateException("audit store down"));

            // account and balance are saved BEFORE the audit write. Without @Transactional on the service,
            // each save would commit on its own and these rows would remain. This test fails in that case.
            assertThatThrownBy(() -> accountService.openInternalAccount("EUR", "1000", "sara", "corr-3"))
                    .hasMessageContaining("audit store down");

            assertThat(count("SELECT count(*) FROM account")).isZero();
            assertThat(count("SELECT count(*) FROM account_balance")).isZero();
        }
    }
}
