// src/main/java/com/moneymanager/service/AnalyticsService.java
package com.moneymanager.service;

import com.moneymanager.model.Account;
import com.moneymanager.model.Transaction;
import com.moneymanager.model.User;
import com.moneymanager.repository.AccountRepository;
import com.moneymanager.repository.TransactionRepository;
import com.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BudgetService budgetService;
    private final GoalService goalService;

    public Map<String, Object> getDashboardAnalytics(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // Get this month's transactions
        List<Transaction> monthlyTransactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), monthStart, monthEnd);

        // Calculate metrics
        BigDecimal monthlyIncome = calculateTotalByType(monthlyTransactions, Transaction.TransactionType.INCOME);
        BigDecimal monthlyExpense = calculateTotalByType(monthlyTransactions, Transaction.TransactionType.EXPENSE);
        BigDecimal monthlyNet = monthlyIncome.subtract(monthlyExpense);

        // Get last month for comparison
        YearMonth lastMonth = currentMonth.minusMonths(1);
        LocalDate lastMonthStart = lastMonth.atDay(1);
        LocalDate lastMonthEnd = lastMonth.atEndOfMonth();

        List<Transaction> lastMonthTransactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), lastMonthStart, lastMonthEnd);

        BigDecimal lastMonthIncome = calculateTotalByType(lastMonthTransactions, Transaction.TransactionType.INCOME);
        BigDecimal lastMonthExpense = calculateTotalByType(lastMonthTransactions, Transaction.TransactionType.EXPENSE);
        BigDecimal lastMonthNet = lastMonthIncome.subtract(lastMonthExpense);

        // Calculate changes
        BigDecimal incomeChange = calculatePercentageChange(lastMonthIncome, monthlyIncome);
        BigDecimal expenseChange = calculatePercentageChange(lastMonthExpense, monthlyExpense);
        BigDecimal netChange = calculatePercentageChange(lastMonthNet, monthlyNet);

        // Get account balances
        List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(user.getId());
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get budget overview
        Map<String, Object> budgetOverview = budgetService.getBudgetOverview(userEmail);

        // Get goals overview
        Map<String, Object> goalsOverview = goalService.getGoalsOverview(userEmail);

        // Recent transactions
        List<Map<String, Object>> recentTransactions = monthlyTransactions.stream()
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .limit(5)
                .map(this::convertTransactionToMap)
                .collect(Collectors.toList());

        // Top expense categories
        Map<String, BigDecimal> topExpenseCategories = monthlyTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().setScale(2, RoundingMode.HALF_UP)
                ));

        Map<String,Object> dashboardData = new HashMap<>();
        dashboardData.put("currentMonth", currentMonth.toString());
        dashboardData.put("monthlyIncome", monthlyIncome.setScale(2, RoundingMode.HALF_UP));
        dashboardData.put("monthlyExpense", monthlyExpense.setScale(2, RoundingMode.HALF_UP));
        dashboardData.put("monthlyNet", monthlyNet.setScale(2, RoundingMode.HALF_UP));
        dashboardData.put("incomeChange", incomeChange);
        dashboardData.put("expenseChange", expenseChange);
        dashboardData.put("netChange", netChange);
        dashboardData.put("totalBalance", totalBalance.setScale(2, RoundingMode.HALF_UP));
        dashboardData.put("accountCount", accounts.size());
        dashboardData.put("transactionCount", monthlyTransactions.size());
        dashboardData.put("savingsRate", monthlyIncome.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                monthlyNet.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP));
        dashboardData.put("budgetOverview", budgetOverview);
        dashboardData.put("goalsOverview", goalsOverview);
        dashboardData.put("recentTransactions", recentTransactions);
        dashboardData.put("topExpenseCategories", topExpenseCategories);
        dashboardData.put("financialHealth", assessFinancialHealth(monthlyNet, monthlyIncome));
        return dashboardData;
    }

    public Map<String, Object> getMonthlyTrends(String userEmail, int months) {
        User user = getUserByEmail(userEmail);
        LocalDate endDate = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        LocalDate startDate = endDate.minusMonths(months - 1).with(TemporalAdjusters.firstDayOfMonth());

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), startDate, endDate);

        // Group by month
        Map<YearMonth, List<Transaction>> transactionsByMonth = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate())
                ));

        // Prepare monthly data
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);

        while (!current.isAfter(end)) {
            List<Transaction> monthTransactions = transactionsByMonth.getOrDefault(current, Collections.emptyList());

            BigDecimal monthIncome = calculateTotalByType(monthTransactions, Transaction.TransactionType.INCOME);
            BigDecimal monthExpense = calculateTotalByType(monthTransactions, Transaction.TransactionType.EXPENSE);
            BigDecimal monthNet = monthIncome.subtract(monthExpense);

            monthlyData.add(Map.of(
                    "month", current.toString(),
                    "income", monthIncome.setScale(2, RoundingMode.HALF_UP),
                    "expense", monthExpense.setScale(2, RoundingMode.HALF_UP),
                    "net", monthNet.setScale(2, RoundingMode.HALF_UP),
                    "transactionCount", monthTransactions.size(),
                    "savingsRate", monthIncome.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                            monthNet.divide(monthIncome, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP)
            ));

            current = current.plusMonths(1);
        }

        // Calculate averages
        BigDecimal avgIncome = monthlyData.stream()
                .map(m -> (BigDecimal) m.get("income"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthlyData.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgExpense = monthlyData.stream()
                .map(m -> (BigDecimal) m.get("expense"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthlyData.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgSavingsRate = monthlyData.stream()
                .map(m -> (BigDecimal) m.get("savingsRate"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(monthlyData.size()), 2, RoundingMode.HALF_UP);

        return Map.of(
                "periodMonths", months,
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "monthlyData", monthlyData,
                "averages", Map.of(
                        "income", avgIncome,
                        "expense", avgExpense,
                        "savingsRate", avgSavingsRate
                ),
                "totalIncome", monthlyData.stream()
                        .map(m -> (BigDecimal) m.get("income"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP),
                "totalExpense", monthlyData.stream()
                        .map(m -> (BigDecimal) m.get("expense"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP),
                "totalNet", monthlyData.stream()
                        .map(m -> (BigDecimal) m.get("net"))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    public Map<String, Object> getCategoryAnalysis(String userEmail, String period) {
        User user = getUserByEmail(userEmail);
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        switch (period.toUpperCase()) {
            case "WEEK":
                startDate = endDate.minusDays(7);
                break;
            case "MONTH":
                startDate = endDate.with(TemporalAdjusters.firstDayOfMonth());
                endDate = endDate.with(TemporalAdjusters.lastDayOfMonth());
                break;
            case "QUARTER":
                startDate = endDate.minusMonths(3);
                break;
            case "YEAR":
                startDate = endDate.with(TemporalAdjusters.firstDayOfYear());
                endDate = endDate.with(TemporalAdjusters.lastDayOfYear());
                break;
            default:
                startDate = endDate.minusMonths(1);
        }

        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), startDate, endDate);

        // Income by category
        Map<String, BigDecimal> incomeByCategory = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().setScale(2, RoundingMode.HALF_UP),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Expense by category
        Map<String, BigDecimal> expenseByCategory = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().setScale(2, RoundingMode.HALF_UP),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentages
        Map<String, Double> incomePercentages = incomeByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> totalIncome.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                                entry.getValue().divide(totalIncome, 4, RoundingMode.HALF_UP)
                                        .doubleValue() * 100
                ));

        Map<String, Double> expensePercentages = expenseByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> totalExpense.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                                entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP)
                                        .doubleValue() * 100
                ));

        Map<String,Object> result = new HashMap<>();
        result.put("period", period);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalIncome", totalIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("totalExpense", totalExpense.setScale(2, RoundingMode.HALF_UP));
        result.put("incomeByCategory", incomeByCategory);
        result.put("expenseByCategory", expenseByCategory);
        result.put("incomePercentages", incomePercentages);
        result.put("expensePercentages", expensePercentages);
        result.put("topIncomeCategory", incomeByCategory.isEmpty() ? "N/A" :
                incomeByCategory.keySet().stream().findFirst().orElse("N/A"));
        result.put("topExpenseCategory", expenseByCategory.isEmpty() ? "N/A" :
                expenseByCategory.keySet().stream().findFirst().orElse("N/A"));

        return result;
    }

    public Map<String, Object> getAccountAnalysis(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(user.getId());

        // Account balances
        List<Map<String, Object>> accountBalances = accounts.stream()
                .map(account -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", account.getId());
                            map.put("name", account.getName());
                            map.put("type", account.getAccountType().name());
                            map.put("balance", account.getBalance().setScale(2, RoundingMode.HALF_UP));
                            map.put("currency", account.getCurrency());
                            return map;
                        }
                ).toList();

        // Balance by account type
        Map<String, BigDecimal> balanceByType = accounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getAccountType().name(),
                        Collectors.reducing(BigDecimal.ZERO, Account::getBalance, BigDecimal::add)
                ));

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Balance changes (would need historical data - simplified)
        Map<String, BigDecimal> balanceChanges = new HashMap<>();
        for (Account account : accounts) {
            // In a real app, you'd compare with previous month's balance
            balanceChanges.put(account.getName(), BigDecimal.ZERO);
        }

        return Map.of(
                "totalAccounts", accounts.size(),
                "totalBalance", totalBalance.setScale(2, RoundingMode.HALF_UP),
                "accountBalances", accountBalances,
                "balanceByType", balanceByType,
                "balanceChanges", balanceChanges,
                "primaryAccount", accounts.isEmpty() ? null :
                        Map.of(
                                "id", accounts.get(0).getId(),
                                "name", accounts.get(0).getName(),
                                "balance", accounts.get(0).getBalance().setScale(2, RoundingMode.HALF_UP)
                        )
        );
    }

    public Map<String, Object> getFinancialHealth(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);

        // Get last 3 months data
        LocalDate threeMonthsAgo = today.minusMonths(3);
        List<Transaction> recentTransactions = transactionRepository
                .findByUserIdAndTransactionDateBetween(user.getId(), threeMonthsAgo, today);

        // Calculate metrics
        BigDecimal avgMonthlyIncome = calculateAverageMonthly(recentTransactions, Transaction.TransactionType.INCOME);
        BigDecimal avgMonthlyExpense = calculateAverageMonthly(recentTransactions, Transaction.TransactionType.EXPENSE);
        BigDecimal avgMonthlySavings = avgMonthlyIncome.subtract(avgMonthlyExpense);

        // Emergency fund (3 months of expenses)
        BigDecimal recommendedEmergencyFund = avgMonthlyExpense.multiply(BigDecimal.valueOf(3));

        // Get current savings
        List<Account> accounts = accountRepository.findByUserIdAndActiveTrue(user.getId());
        BigDecimal totalSavings = accounts.stream()
                .filter(a -> a.getAccountType() == Account.AccountType.SAVINGS)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Savings rate
        BigDecimal savingsRate = avgMonthlyIncome.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO :
                avgMonthlySavings.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

        // Get budget adherence
        Map<String, Object> budgetOverview = budgetService.getBudgetOverview(userEmail);

        // FIX 1: Safe Casting using Number
        Number overBudgetNum = (Number) budgetOverview.get("overBudgetCount");
        Number totalBudgetsNum = (Number) budgetOverview.get("totalBudgets");

        int overBudgetCount = overBudgetNum != null ? overBudgetNum.intValue() : 0;
        int totalBudgets = totalBudgetsNum != null ? totalBudgetsNum.intValue() : 0;

        double budgetAdherence = totalBudgets == 0 ? 100 :
                100 - ((double) overBudgetCount / totalBudgets * 100);

        // Get goals progress
        Map<String, Object> goalsOverview = goalService.getGoalsOverview(userEmail);

        // FIX 2: Safe Casting for Double
        Number progressNum = (Number) goalsOverview.get("overallProgress");
        double overallGoalsProgress = progressNum != null ? progressNum.doubleValue() : 0.0;

        // Calculate score (0-100)
        int score = calculateFinancialHealthScore(
                savingsRate.doubleValue(),
                budgetAdherence,
                overallGoalsProgress,
                totalSavings.compareTo(recommendedEmergencyFund) >= 0
        );

        String healthStatus = getHealthStatus(score);
        List<String> recommendations = generateRecommendations(
                savingsRate.doubleValue(),
                budgetAdherence,
                totalSavings.compareTo(recommendedEmergencyFund) < 0
        );

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("status", healthStatus);
        result.put("savingsRate", savingsRate.setScale(2, RoundingMode.HALF_UP));
        result.put("budgetAdherence", budgetAdherence);
        result.put("goalsProgress", overallGoalsProgress);

        Map<String, Object> emergencyMap = new HashMap<>();
        emergencyMap.put("current", totalSavings.setScale(2, RoundingMode.HALF_UP));
        emergencyMap.put("recommended", recommendedEmergencyFund.setScale(2, RoundingMode.HALF_UP));
        emergencyMap.put("hasEnough", totalSavings.compareTo(recommendedEmergencyFund) >= 0);

        // FIX 3: Safe Division (Check if recommended is zero)
        BigDecimal percentage;
        if (recommendedEmergencyFund.compareTo(BigDecimal.ZERO) == 0) {
            // If expenses are 0, and you have savings, you are 100% covered. If no savings, 0%.
            percentage = totalSavings.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        } else {
            percentage = totalSavings.divide(recommendedEmergencyFund, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        emergencyMap.put("percentage", percentage);
        result.put("emergencyFundStatus", emergencyMap);

        Map<String, Object> monthlyMetricsMap = new HashMap<>();
        monthlyMetricsMap.put("income", avgMonthlyIncome.setScale(2, RoundingMode.HALF_UP));
        monthlyMetricsMap.put("expense", avgMonthlyExpense.setScale(2, RoundingMode.HALF_UP));
        monthlyMetricsMap.put("savings", avgMonthlySavings.setScale(2, RoundingMode.HALF_UP));
        result.put("monthlyMetrics", monthlyMetricsMap);

        result.put("recommendations", recommendations);
        result.put("lastUpdated", today.toString());

        return result;
    }
    // ========== HELPER METHODS ==========

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private BigDecimal calculateTotalByType(List<Transaction> transactions, Transaction.TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return newValue.subtract(oldValue)
                .divide(oldValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageMonthly(List<Transaction> transactions, Transaction.TransactionType type) {
        // Group by month
        Map<YearMonth, BigDecimal> monthlyTotals = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTransactionDate()),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        if (monthlyTotals.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = monthlyTotals.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(monthlyTotals.size()), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> convertTransactionToMap(Transaction transaction) {
        return Map.of(
                "id", transaction.getId(),
                "title", transaction.getTitle(),
                "amount", transaction.getAmount().setScale(2, RoundingMode.HALF_UP),
                "type", transaction.getType().name(),
                "date", transaction.getTransactionDate().toString(),
                "category", transaction.getCategory() != null ? transaction.getCategory().getName() : "Uncategorized",
                "account", transaction.getAccount() != null ? transaction.getAccount().getName() : "No Account"
        );
    }

    private String assessFinancialHealth(BigDecimal monthlyNet, BigDecimal monthlyIncome) {
        if (monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return "NO_INCOME";
        }

        double savingsRate = monthlyNet.divide(monthlyIncome, 4, RoundingMode.HALF_UP).doubleValue();

        if (savingsRate >= 0.2) return "EXCELLENT";
        if (savingsRate >= 0.1) return "GOOD";
        if (savingsRate >= 0) return "FAIR";
        return "NEEDS_IMPROVEMENT";
    }

    private int calculateFinancialHealthScore(double savingsRate, double budgetAdherence,
                                              double goalsProgress, boolean hasEmergencyFund) {
        int score = 0;

        // Savings rate (0-30 points)
        if (savingsRate >= 20) score += 30;
        else if (savingsRate >= 10) score += 20;
        else if (savingsRate > 0) score += 10;

        // Budget adherence (0-25 points)
        score += (int) (budgetAdherence * 0.25);

        // Goals progress (0-20 points)
        score += (int) (goalsProgress * 0.20);

        // Emergency fund (0-25 points)
        if (hasEmergencyFund) score += 25;
        else if (goalsProgress > 50) score += 15;
        else score += 5;

        return Math.min(score, 100);
    }

    private String getHealthStatus(int score) {
        if (score >= 80) return "EXCELLENT";
        if (score >= 60) return "GOOD";
        if (score >= 40) return "FAIR";
        return "NEEDS_IMPROVEMENT";
    }

    private List<String> generateRecommendations(double savingsRate, double budgetAdherence,
                                                 boolean needsEmergencyFund) {
        List<String> recommendations = new ArrayList<>();

        if (savingsRate < 10) {
            recommendations.add("Try to increase your savings rate to at least 10% of income.");
        }

        if (budgetAdherence < 80) {
            recommendations.add("Review your budgets and identify categories where you're overspending.");
        }

        if (needsEmergencyFund) {
            recommendations.add("Build an emergency fund covering 3-6 months of expenses.");
        }

        if (savingsRate > 20 && budgetAdherence > 90) {
            recommendations.add("Consider investing your surplus savings for long-term growth.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("You're doing great! Consider setting more challenging financial goals.");
        }

        return recommendations;
    }
}