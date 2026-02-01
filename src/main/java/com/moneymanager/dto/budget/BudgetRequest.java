package com.moneymanager.dto.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String period,
        Long categoryId
) {}