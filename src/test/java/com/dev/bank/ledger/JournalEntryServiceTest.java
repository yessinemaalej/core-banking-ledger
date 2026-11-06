package com.dev.bank.ledger;

import com.dev.bank.TestcontainersConfiguration;
import com.dev.bank.account.AccountNotFoundException;
import com.dev.bank.audit.AuditLogRepository;
import com.dev.bank.money.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Proves the service's real transaction against Postgres: no @Transactional on these tests,
 * so every call really commits or rolls back. Commits are real, so every test cleans up in @AfterEach.
 */
class JournalEntryServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @SpringBootTest
    @Import(TestcontainersConfiguration.class)
    abstract static class WithDatabase {

        @Autowired
        JournalEntryService journalEntryService;
        @Autowired
        JdbcTemplate jdbc;

        UUID a;
        UUID b;

        @BeforeEach
        void insertAccounts() {
            jdbc.update("INSERT INTO currency VALUES ('EUR', 2, true)");
            jdbc.update("INSERT INTO gl_account VALUES ('1000', 'Cash', 'ASSET', 'DEBIT')");
            a = insertAccount("OPEN");
            b = insertAccount("OPEN");
        }

        @AfterEach
        void cleanUp() {
            jdbc.execute("TRUNCATE posting, journal_entry, audit_log, account_balance, account, gl_account, currency CASCADE");
        }

        UUID insertAccount(String status) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO account (id, currency_code, gl_account_code, type, status)"
                    + " VALUES (?, 'EUR', '1000', 'INTERNAL', ?)", id, status);
            jdbc.update("INSERT INTO account_balance (account_id) VALUES (?)", id);
            return id;
        }

        PostedEntry transfer(UUID debited, UUID credited, long amountMinor) {
            return journalEntryService.postTransfer(TODAY, TODAY, List.of(
                            new TransferLine(debited, eur(amountMinor)), new TransferLine(credited, eur(-amountMinor))),
                    "sara", "corr-1", null);
        }

        long balance(UUID accountId) {
            return jdbc.queryForObject("SELECT balance_minor FROM account_balance WHERE account_id = ?", Long.class, accountId);
        }

        long sumOfPostings(UUID accountId) {
            return jdbc.queryForObject("SELECT COALESCE(SUM(amount_minor), 0) FROM posting WHERE account_id = ?",
                    Long.class, accountId);
        }

        long count(String sql) {
            return jdbc.queryForObject(sql, Long.class);
        }

        static Money eur(long amountMinor) {
            return new Money(amountMinor, "EUR");
        }
    }

    @Nested
    class WhenTheEntryIsValid extends WithDatabase {

        @Test
        void commitsEntryPostingsBalancesAndAudit() {
            PostedEntry posted = transfer(a, b, 1050);

            UUID entryId = posted.entry().getId();
            assertThat(count("SELECT count(*) FROM journal_entry")).isEqualTo(1);
            assertThat(count("SELECT count(*) FROM posting")).isEqualTo(2);
            assertThat(balance(a)).isEqualTo(1050);
            assertThat(balance(b)).isEqualTo(-1050);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'JOURNAL_ENTRY_POSTED'"
                    + " AND object_id = ? AND actor = 'sara' AND before IS NULL"
                    + " AND jsonb_array_length(after -> 'postings') = 2", Long.class, entryId.toString())).isEqualTo(1);
        }

        @Test
        void sameAccountOnTwoLinesGetsTwoPostingsAndOneBalanceUpdate() {
            journalEntryService.postTransfer(TODAY, TODAY, List.of(
                    new TransferLine(a, eur(500)), new TransferLine(a, eur(550)), new TransferLine(b, eur(-1050))),
                    "sara", "corr-2", null);

            assertThat(count("SELECT count(*) FROM posting")).isEqualTo(3);
            assertThat(balance(a)).isEqualTo(1050);
            assertThat(balance(b)).isEqualTo(-1050);
        }
    }

    @Nested
    class WhenTheEntryIsRefused extends WithDatabase {

        @Test
        void unknownAccountIsReportedAndNothingIsWritten() {
            UUID unknown = UUID.randomUUID();

            assertThatThrownBy(() -> transfer(a, unknown, 1050))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(unknown.toString());

            assertThat(count("SELECT count(*) FROM journal_entry")).isZero();
            assertThat(balance(a)).isZero();
        }

        @Test
        void unbalancedEntryLeavesBalancesUntouched() {
            assertThatThrownBy(() -> journalEntryService.postTransfer(TODAY, TODAY, List.of(
                    new TransferLine(a, eur(1050)), new TransferLine(b, eur(-1000))), "sara", "corr-3", null))
                    .isInstanceOf(UnbalancedJournalEntryException.class);

            assertThat(count("SELECT count(*) FROM journal_entry")).isZero();
            assertThat(balance(a)).isZero();
            assertThat(balance(b)).isZero();
        }
    }

    @Nested
    class WhenTheAuditWriteFails extends WithDatabase {

        // replaces the real repository in this context only: every audit save throws
        @MockitoBean
        AuditLogRepository auditLogRepository;

        @Test
        void entryPostingsAndBalancesAreRolledBack() {
            when(auditLogRepository.save(any())).thenThrow(new IllegalStateException("audit store down"));

            // the audit row is written last: without @Transactional, entry, postings and balances would remain
            assertThatThrownBy(() -> transfer(a, b, 1050)).hasMessageContaining("audit store down");

            assertThat(count("SELECT count(*) FROM journal_entry")).isZero();
            assertThat(count("SELECT count(*) FROM posting")).isZero();
            assertThat(balance(a)).isZero();
            assertThat(balance(b)).isZero();
        }
    }

    @Nested
    class UnderConcurrency extends WithDatabase {

        @Test
        void concurrentTransfersInBothDirectionsLoseNothingAndNeverDeadlock() throws Exception {
            int aToB = 30;
            int bToA = 10;
            ExecutorService pool = Executors.newFixedThreadPool(8);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < aToB + bToA; i++) {
                boolean fromA = i < aToB;
                results.add(pool.submit(() -> {
                    start.await();  // release all threads at once to maximise overlap
                    return fromA ? transfer(a, b, 1) : transfer(b, a, 1);
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                result.get();  // rethrows a lost deadlock victim or any other failure
            }
            pool.shutdown();

            // without FOR UPDATE, concurrent read-modify-writes would lose some of these updates
            assertThat(balance(a)).isEqualTo(aToB - bToA);
            assertThat(balance(b)).isEqualTo(bToA - aToB);
            // the cached balance must always equal the sum of the account's postings
            assertThat(balance(a)).isEqualTo(sumOfPostings(a));
            assertThat(balance(b)).isEqualTo(sumOfPostings(b));
        }
    }
}
