// src/main/java/com/moneymanager/dto/BudgetDTO.java
package com.moneymanager.dto.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BudgetDTO(
        Long id,

        @NotBlank(message = "Budget name is required")
        String name,

        String description,

        @NotNull(message = "Budget amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        BigDecimal spentAmount,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotBlank(message = "Period is required")
        String period,

        Long categoryId,

        Long userId,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        boolean active,

        // Calculated fields
        BigDecimal remainingAmount,

        double percentageUsed
) {}

