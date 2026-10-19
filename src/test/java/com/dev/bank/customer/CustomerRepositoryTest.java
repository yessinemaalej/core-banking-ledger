package com.dev.bank.customer;

import com.dev.bank.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.show-sql=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CustomerRepositoryTest {

    @Autowired
    CustomerRepository repository;
    @Autowired
    TestEntityManager entityManager;

    @Test
    void roundTrip() {
        Customer customer = Customer.create();
        repository.save(customer);
        entityManager.flush();
        entityManager.clear();

        Customer loaded = repository.findById(customer.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(CustomerStatus.UNVERIFIED);
        assertThat(customer.getCreatedAt()).isEqualTo(loaded.getCreatedAt());
        assertThat(customer.getId()).isEqualTo(loaded.getId());
    }
}