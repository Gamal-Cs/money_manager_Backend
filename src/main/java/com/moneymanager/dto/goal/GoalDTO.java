// src/main/java/com/moneymanager/dto/GoalDTO.java
package com.moneymanager.dto.goal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalDTO(
        Long id,

        @NotBlank(message = "Goal name is required")
        String name,

        String description,

        @NotNull(message = "Target amount is required")
        @Positive(message = "Target amount must be positive")
        BigDecimal targetAmount,

        BigDecimal currentAmount,

        @NotNull(message = "Target date is required")
        LocalDate targetDate,

        LocalDate startDate,

        Long accountId,

        Long userId,

        String status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        String icon,

        String color,

        // Calculated fields
        BigDecimal remainingAmount,

        double progressPercentage,

        long daysRemaining,

        BigDecimal dailyRequired
) {}

