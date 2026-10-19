package com.dev.bank.customer;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface CustomerRepository extends Repository<Customer, UUID> {
    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);
    
}
