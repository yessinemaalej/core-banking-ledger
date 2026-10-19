package com.dev.bank.account;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Table(name = "account")
public class Account implements Persistable<UUID>{

    @Id
    @Column(name="id")
    private UUID id;

    @Nullable
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "gl_account_code")
    private String glAccountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccountStatus accountStatus;


    @Column(name = "created_at")
    private Instant createdAt;



    @Transient
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return id;
    }


    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    protected Account(){
        //for jpa
    }

    private Account(UUID id, @Nullable UUID customerId, AccountType accountType, AccountStatus accountStatus, Instant createdAt, String glAccountCode, String currencyCode){
        this.id = id;
        this.accountType = accountType;
        this.createdAt = createdAt;
        this.accountStatus = accountStatus;
        this.glAccountCode = glAccountCode;
        this.currencyCode = currencyCode;
        this.customerId = customerId;
    }

    public static Account createInternal(String currencyCode, String glAccountCode){
        UUID id = UUID.randomUUID();
        AccountStatus accountStatus = AccountStatus.OPEN;
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new Account(id, null,AccountType.INTERNAL, accountStatus, createdAt, glAccountCode, currencyCode);


    }

    public static Account createCustomerAccount(String currencyCode, UUID customerId ,String glAccountCode){
        Objects.requireNonNull(customerId, "customerId is required for a customer deposit account");
        UUID id = UUID.randomUUID();
        AccountStatus accountStatus = AccountStatus.OPEN;

        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return new Account(id, customerId,AccountType.CUSTOMER_DEPOSIT, accountStatus, createdAt, glAccountCode, currencyCode);

    }


}
