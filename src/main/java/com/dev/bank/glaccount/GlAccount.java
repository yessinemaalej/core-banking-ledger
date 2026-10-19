package com.dev.bank.glaccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table (name = "gl_account")
public class GlAccount {
    @Id
    @Column(name = "code", nullable = false) 
    private String code;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column (name = "class")
    private AccountClass accountClass;

    @Enumerated(EnumType.STRING)
    @Column (name = "normal_side")
    private NormalSide normalSide;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AccountClass getAccountClass() {
        return accountClass;
    }

    public NormalSide getNormalSide() {
        return normalSide;
    }

    protected GlAccount() {
        // for JPA
    }

    public GlAccount(String code, String name, AccountClass accountClass, NormalSide normalSide) {
        this.code = code;
        this.name = name;
        this.accountClass = accountClass;
        this.normalSide = normalSide;
    }


}
