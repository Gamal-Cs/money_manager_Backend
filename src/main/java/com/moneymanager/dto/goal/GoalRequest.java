package com.moneymanager.dto.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal targetAmount,
        @NotNull LocalDate targetDate,
        Long accountId,
        String icon,
        String color
) {}