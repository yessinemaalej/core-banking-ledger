package com.dev.bank.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "currency")
public class Currency {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "minor_unit_exponent", nullable = false)
    private long minorUnitExponent;

    @Column(name = "active", nullable = false)
    private boolean active;


}
