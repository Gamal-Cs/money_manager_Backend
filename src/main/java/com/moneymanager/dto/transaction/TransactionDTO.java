// src/main/java/com/moneymanager/dto/TransactionDTO.java
package com.moneymanager.dto.transaction;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionDTO(
        Long id,

        @NotBlank(message = "Title is required")
        @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
        String title,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Transaction type is required")
        @Pattern(regexp = "INCOME|EXPENSE", message = "Type must be INCOME or EXPENSE")
        String type,

        @NotNull(message = "Transaction date is required")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate transactionDate,

        @NotNull(message = "User ID is required")
        Long userId,

        Long accountId, // ADD THIS

        Long categoryId, // ADD THIS

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate createdAt
) {
    // Static factory method for creating without ID (for new transactions)
    public static TransactionDTO create(
            String title,
            String description,
            BigDecimal amount,
            String type,
            LocalDate transactionDate,
            Long userId,
            Long accountId,
            Long categoryId
    ) {
        return new TransactionDTO(
                null,
                title,
                description,
                amount,
                type,
                transactionDate,
                userId,
                accountId,
                categoryId,
                null
        );
    }
}