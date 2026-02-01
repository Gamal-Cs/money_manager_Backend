// src/main/java/com/moneymanager/dto/AccountDTO.java
package com.moneymanager.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountDTO(
        Long id,

        @NotBlank(message = "Account name is required")
        String name,

        @NotBlank(message = "Account type is required")
        String accountType,

        String accountNumber,

        @NotNull(message = "Balance is required")
        @PositiveOrZero(message = "Balance cannot be negative")
        BigDecimal balance,

        @NotBlank(message = "Currency is required")
        String currency,

        String description,

        Long userId,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        boolean active
) {}

