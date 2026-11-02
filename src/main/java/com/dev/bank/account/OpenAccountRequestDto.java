package com.dev.bank.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record OpenAccountRequestDto(

    @NotBlank(message = "currencyCode is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be 3 uppercase letters, for example EUR")
    String currencyCode,

    @NotBlank(message = "glAccountCode is required")
    String glAccountCode,

    UUID customerId
){}
