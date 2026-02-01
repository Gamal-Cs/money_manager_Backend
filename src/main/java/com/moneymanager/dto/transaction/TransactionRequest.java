// src/main/java/com/moneymanager/dto/transaction/TransactionRequest.java
package com.moneymanager.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
public record TransactionRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "Transaction type is required")
        String type, // "INCOME" or "EXPENSE"

        @NotNull(message = "Transaction date is required")
        LocalDate transactionDate,

         Long accountId,
         Long categoryId
) {}