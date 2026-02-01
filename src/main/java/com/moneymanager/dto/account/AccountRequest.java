package com.moneymanager.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank String name,
        @NotBlank String accountType,
        String accountNumber,
        @NotNull BigDecimal balance,
        @NotBlank String currency,
        String description
) {}