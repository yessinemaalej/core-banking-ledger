package com.dev.bank.account;

import com.dev.bank.TestcontainersConfiguration;
import com.dev.bank.customer.CustomerStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /accounts through the whole stack: JSON → validation → controller → service → Postgres, and back.
 * Checks status codes, the JSON body, and for errors that nothing was written.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void insertReferenceData() {
        jdbc.update("INSERT INTO currency VALUES ('EUR', 2, true)");
        jdbc.update("INSERT INTO gl_account VALUES ('1000', 'Cash', 'ASSET', 'DEBIT')");
        jdbc.update("INSERT INTO gl_account VALUES ('2000', 'Customer deposits', 'LIABILITY', 'CREDIT')");
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("TRUNCATE audit_log, account_balance, account, customer, gl_account, currency CASCADE");
    }

    @Test
    void opensInternalAccountAndReturns201WithTheAccountId() throws Exception {
        MvcResult result = mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currencyCode": "EUR", "glAccountCode": "1000"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INTERNAL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.currencyCode").value("EUR"))
                .andExpect(jsonPath("$.customerId").doesNotExist())
                .andReturn();

        // the returned id must be the id of the row really created
        String id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(count("SELECT count(*) FROM account WHERE id = ?::uuid", id)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM audit_log WHERE object_id = ? AND actor = 'system'", id)).isEqualTo(1);
    }

    @Test
    void opensCustomerAccountAndReturnsTheAccountIdNotTheCustomerId() throws Exception {
        UUID customerId = insertCustomer(CustomerStatus.VERIFIED);

        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currencyCode": "EUR", "glAccountCode": "2000", "customerId": "%s"}
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CUSTOMER_DEPOSIT"))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.id").value(not(customerId.toString())));
    }

    @Test
    void unverifiedCustomerGets403ProblemDetailAndNothingIsWritten() throws Exception {
        UUID customerId = insertCustomer(CustomerStatus.PENDING);

        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currencyCode": "EUR", "glAccountCode": "2000", "customerId": "%s"}
                                """.formatted(customerId)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(containsString("PENDING")));

        assertThat(count("SELECT count(*) FROM account")).isZero();
    }

    @Test
    void unknownCustomerGets404ProblemDetail() throws Exception {
        UUID unknown = UUID.randomUUID();

        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currencyCode": "EUR", "glAccountCode": "2000", "customerId": "%s"}
                                """.formatted(unknown)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString(unknown.toString())));
    }

    @Test
    void invalidCurrencyCodeGets400BeforeReachingTheService() throws Exception {
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currencyCode": "eur", "glAccountCode": "1000"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(count("SELECT count(*) FROM account")).isZero();
    }

    @Test
    void clientCannotChooseTheAccountIdOrStatus() throws Exception {
        UUID chosenId = UUID.randomUUID();

        // mass assignment attempt: unknown fields are ignored because the request record does not declare them
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id": "%s", "status": "FROZEN", "currencyCode": "EUR", "glAccountCode": "1000"}
                                """.formatted(chosenId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(not(chosenId.toString())))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    private UUID insertCustomer(CustomerStatus status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO customer (id, status) VALUES (?, ?)", id, status.name());
        return id;
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}
