// src/main/java/com/moneymanager/dto/transaction/TransactionSummary.java
package com.moneymanager.dto.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public record TransactionSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount,
        LocalDate startDate,
        LocalDate endDate,
        int incomeCount,
        int expenseCount,
        BigDecimal averageIncome,
        BigDecimal averageExpense,
        Map<String, BigDecimal> topIncomeCategories,
        Map<String, BigDecimal> topExpenseCategories
) {
    public BigDecimal getIncomeExpenseRatio() {
        if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalIncome.divide(totalExpense, 2, RoundingMode.HALF_UP);
    }

    public String getFinancialHealth() {
        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "HEALTHY";
        } else if (netAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "BALANCED";
        } else {
            return "DEFICIT";
        }
    }

    public BigDecimal getSavingsRate() {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return netAmount.divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getExpenseToIncomeRatio() {
        if (totalIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalExpense.divide(totalIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}