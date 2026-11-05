package com.dev.bank.web;

import com.dev.bank.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JsonAmountTest {

    // a tiny record, only for this test
    record AmountRequest(long amountMinor, String currency) {}

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void decimalAmountIsRejected() {
        String json = """
                {"amountMinor": 10.5, "currency": "EUR"}
                """;

        // without the property, Jackson would silently truncate 10.5 to 10
        assertThatThrownBy(() -> jsonMapper.readValue(json, AmountRequest.class))
                .isInstanceOf(InvalidFormatException.class);

    }
}
