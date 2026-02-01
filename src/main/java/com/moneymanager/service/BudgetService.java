// src/main/java/com/moneymanager/service/BudgetService.java
package com.moneymanager.service;

import com.moneymanager.dto.budget.BudgetDTO;
import com.moneymanager.dto.budget.BudgetRequest;
import com.moneymanager.model.Budget;
import com.moneymanager.model.Category;
import com.moneymanager.model.User;
import com.moneymanager.repository.BudgetRepository;
import com.moneymanager.repository.CategoryRepository;
import com.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    // ========== CRUD OPERATIONS ==========

    public BudgetDTO createBudget(String userEmail, BudgetRequest request) {
        User user = getUserByEmail(userEmail);

        // Validate dates
        if (request.startDate().isAfter(request.endDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        // Validate period
        Budget.Period period;
        try {
            period = Budget.Period.valueOf(request.period().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid budget period");
        }

        // Get category if provided
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            // For category budgets, validate it's an expense category
            if (category.getType() != Category.Type.EXPENSE) {
                throw new RuntimeException("Budgets can only be created for expense categories");
            }
        }

        Budget budget = Budget.builder()
                .name(request.name())
                .description(request.description())
                .amount(request.amount())
                .spentAmount(BigDecimal.ZERO)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .period(period)
                .category(category)
                .user(user)
                .active(true)
                .build();

        Budget savedBudget = budgetRepository.save(budget);

        // Calculate initial spent amount
        updateBudgetSpentAmount(savedBudget);

        return convertToDTO(savedBudget);
    }

    public BudgetDTO updateBudget(Long budgetId, String userEmail, BudgetRequest request) {
        User user = getUserByEmail(userEmail);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, user.getId())
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        // Validate dates
        if (request.startDate().isAfter(request.endDate())) {
            throw new RuntimeException("Start date cannot be after end date");
        }

        // Get category if provided
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findByIdAndUserId(request.categoryId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            if (category.getType() != Category.Type.EXPENSE) {
                throw new RuntimeException("Budgets can only be created for expense categories");
            }
        }

        budget.setName(request.name());
        budget.setDescription(request.description());
        budget.setAmount(request.amount());
        budget.setStartDate(request.startDate());
        budget.setEndDate(request.endDate());
        budget.setPeriod(Budget.Period.valueOf(request.period().toUpperCase()));
        budget.setCategory(category);

        Budget updatedBudget = budgetRepository.save(budget);

        // Recalculate spent amount
        updateBudgetSpentAmount(updatedBudget);

        return convertToDTO(updatedBudget);
    }

    public void deleteBudget(Long budgetId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, user.getId())
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        budgetRepository.delete(budget);
    }

    public BudgetDTO getBudgetById(Long budgetId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, user.getId())
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        // Update spent amount before returning
        updateBudgetSpentAmount(budget);

        return convertToDTO(budget);
    }

    // ========== QUERY OPERATIONS ==========

    public List<BudgetDTO> getUserBudgets(String userEmail) {
        User user = getUserByEmail(userEmail);
        return budgetRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(budget -> {
                    updateBudgetSpentAmount(budget);
                    return convertToDTO(budget);
                })
                .collect(Collectors.toList());
    }

    public List<BudgetDTO> getActiveBudgets(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDate today = LocalDate.now();

        return budgetRepository.findActiveBudgetsByUserAndDate(user.getId(), today)
                .stream()
                .map(budget -> {
                    updateBudgetSpentAmount(budget);
                    return convertToDTO(budget);
                })
                .collect(Collectors.toList());
    }

    public List<BudgetDTO> getBudgetsByCategory(String userEmail, Long categoryId) {
        User user = getUserByEmail(userEmail);

        // Verify category belongs to user
        categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return budgetRepository.findByUserIdAndCategoryId(user.getId(), categoryId)
                .stream()
                .map(budget -> {
                    updateBudgetSpentAmount(budget);
                    return convertToDTO(budget);
                })
                .collect(Collectors.toList());
    }

    // ========== BUDGET ANALYSIS ==========

    public Map<String, Object> getBudgetProgress(String userEmail, Long budgetId) {
        User user = getUserByEmail(userEmail);
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, user.getId())
                .orElseThrow(() -> new RuntimeException("Budget not found"));

        // Ensure spent amount is up to date
        updateBudgetSpentAmount(budget);

        BigDecimal remaining = budget.getRemainingAmount();
        double percentageUsed = budget.getPercentageUsed();
        boolean isOverBudget = budget.isOverBudget();

        // Calculate daily/monthly spending rate
        long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(
                budget.getStartDate(), budget.getEndDate()) + 1;
        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(
                budget.getStartDate(), LocalDate.now()) + 1;

        if (daysElapsed < 1) daysElapsed = 1;
        if (daysElapsed > daysInPeriod) daysElapsed = daysInPeriod;

        BigDecimal dailyBudget = budget.getAmount().divide(
                BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
        BigDecimal dailySpent = budget.getSpentAmount().divide(
                BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);

        // Calculate projected spending
        BigDecimal projectedSpent = dailySpent.multiply(BigDecimal.valueOf(daysInPeriod));
        Map<String, Object> response = new HashMap<>();
        response.put("budgetId", budgetId);
        response.put("budgetName", budget.getName());
        response.put("totalAmount", budget.getAmount());
        response.put("spentAmount", budget.getSpentAmount());
        response.put("remainingAmount", remaining);
        response.put("percentageUsed", percentageUsed);
        response.put("isOverBudget", isOverBudget);
        response.put("daysElapsed", daysElapsed);
        response.put("daysRemaining", daysInPeriod - daysElapsed);
        response.put("dailyBudget", dailyBudget);
        response.put("dailySpent", dailySpent);
        response.put("projectedSpent", projectedSpent);
        response.put("status", getBudgetStatus(budget));

        return response;
    }

    public Map<String, Object> getBudgetOverview(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDate today = LocalDate.now();

        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsByUserAndDate(user.getId(), today);

        // Update spent amounts
        activeBudgets.forEach(this::updateBudgetSpentAmount);

        BigDecimal totalBudget = activeBudgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = activeBudgets.stream()
                .map(Budget::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = totalBudget.subtract(totalSpent);

        long onTrackCount = activeBudgets.stream()
                .filter(b -> !b.isOverBudget())
                .count();

        long overBudgetCount = activeBudgets.stream()
                .filter(Budget::isOverBudget)
                .count();

        // Get category breakdown
        Map<String, BigDecimal> budgetByCategory = activeBudgets.stream()
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Budget::getAmount, BigDecimal::add)
                ));

        Map<String, BigDecimal> spentByCategory = activeBudgets.stream()
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Budget::getSpentAmount, BigDecimal::add)
                ));

        return Map.of(
                "totalBudgets", activeBudgets.size(),
                "totalBudgetAmount", totalBudget.setScale(2, RoundingMode.HALF_UP),
                "totalSpentAmount", totalSpent.setScale(2, RoundingMode.HALF_UP),
                "totalRemainingAmount", totalRemaining.setScale(2, RoundingMode.HALF_UP),
                "overallPercentageUsed", totalBudget.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                        totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP).doubleValue() * 100,
                "onTrackCount", onTrackCount,
                "overBudgetCount", overBudgetCount,
                "budgetByCategory", budgetByCategory,
                "spentByCategory", spentByCategory,
                "budgets", activeBudgets.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
        );
    }

    // ========== HELPER METHODS ==========

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private void updateBudgetSpentAmount(Budget budget) {
        BigDecimal spentAmount = BigDecimal.ZERO;

        if (budget.getCategory() != null) {
            // Category-specific budget: sum expenses in that category within budget period
            spentAmount = transactionService.getTransactionsWithFilters(
                            budget.getUser().getEmail(),
                            com.moneymanager.model.Transaction.TransactionType.EXPENSE,
                            budget.getStartDate(),
                            budget.getEndDate(),
                            null, // any account
                            budget.getCategory().getId()
                    ).stream()
                    .map(t -> t.amount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            // General budget: sum all expenses within budget period
            spentAmount = transactionService.getTransactionsWithFilters(
                            budget.getUser().getEmail(),
                            com.moneymanager.model.Transaction.TransactionType.EXPENSE,
                            budget.getStartDate(),
                            budget.getEndDate(),
                            null, // any account
                            null  // any category
                    ).stream()
                    .map(t -> t.amount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        budget.setSpentAmount(spentAmount);
        budgetRepository.save(budget);
    }

    private String getBudgetStatus(Budget budget) {
        double percentage = budget.getPercentageUsed();

        if (percentage >= 100) {
            return "OVER_BUDGET";
        } else if (percentage >= 80) {
            return "NEAR_LIMIT";
        } else if (percentage >= 50) {
            return "MODERATE";
        } else {
            return "UNDER_BUDGET";
        }
    }

    private BudgetDTO convertToDTO(Budget budget) {
        return new BudgetDTO(
                budget.getId(),
                budget.getName(),
                budget.getDescription(),
                budget.getAmount(),
                budget.getSpentAmount(),
                budget.getStartDate(),
                budget.getEndDate(),
                budget.getPeriod().name(),
                budget.getCategory() != null ? budget.getCategory().getId() : null,
                budget.getUser().getId(),
                budget.getCreatedAt(),
                budget.getUpdatedAt(),
                budget.isActive(),
                budget.getRemainingAmount(),
                budget.getPercentageUsed()
        );
    }
}