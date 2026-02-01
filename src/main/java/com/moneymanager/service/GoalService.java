// java
// src/main/java/com/moneymanager/service/GoalService.java
package com.moneymanager.service;

import com.moneymanager.dto.goal.GoalDTO;
import com.moneymanager.dto.goal.GoalRequest;
import com.moneymanager.model.Account;
import com.moneymanager.model.Goal;
import com.moneymanager.model.User;
import com.moneymanager.repository.AccountRepository;
import com.moneymanager.repository.GoalRepository;
import com.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public GoalDTO createGoal(String userEmail, GoalRequest request) {
        User user = getUserByEmail(userEmail);

        // Validate target date
        if (request.targetDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Target date cannot be in the past");
        }

        // Get account if provided
        Account account = null;
        if (request.accountId() != null) {
            account = accountRepository.findByIdAndUserId(request.accountId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
        }

        Goal goal = Goal.builder()
                .name(request.name())
                .description(request.description())
                .targetAmount(request.targetAmount())
                .currentAmount(BigDecimal.ZERO)
                .targetDate(request.targetDate())
                .startDate(LocalDate.now())
                .account(account)
                .user(user)
                .status(Goal.Status.IN_PROGRESS)
                .icon(request.icon())
                .color(request.color())
                .build();

        Goal savedGoal = goalRepository.save(goal);
        return convertToDTO(savedGoal);
    }

    public GoalDTO updateGoal(Long goalId, String userEmail, GoalRequest request) {
        User user = getUserByEmail(userEmail);
        Goal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Can't update completed or abandoned goals
        if (goal.getStatus() != Goal.Status.IN_PROGRESS) {
            throw new RuntimeException("Cannot update completed or abandoned goals");
        }

        // Get account if provided
        Account account = null;
        if (request.accountId() != null) {
            account = accountRepository.findByIdAndUserId(request.accountId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Account not found"));
        }

        goal.setName(request.name());
        goal.setDescription(request.description());
        goal.setTargetAmount(request.targetAmount());
        goal.setTargetDate(request.targetDate());
        goal.setAccount(account);
        goal.setIcon(request.icon());
        goal.setColor(request.color());

        // Auto-update status based on new target
        updateGoalStatus(goal);

        Goal updatedGoal = goalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    public void deleteGoal(Long goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Goal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goalRepository.delete(goal);
    }

    public GoalDTO getGoalById(Long goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Goal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        return convertToDTO(goal);
    }

    public GoalDTO addToGoal(Long goalId, String userEmail, BigDecimal amount) {
        User user = getUserByEmail(userEmail);
        Goal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (goal.getStatus() != Goal.Status.IN_PROGRESS) {
            throw new RuntimeException("Cannot add to completed or abandoned goals");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        BigDecimal newAmount = goal.getCurrentAmount().add(amount);

        if (newAmount.compareTo(goal.getTargetAmount()) > 0) {
            throw new RuntimeException("Amount exceeds remaining goal amount");
        }

        goal.setCurrentAmount(newAmount);

        if (goal.getAccount() != null) {
            Account account = goal.getAccount();
            account.setBalance(account.getBalance().add(amount));
            accountRepository.save(account);
        }

        updateGoalStatus(goal);

        Goal updatedGoal = goalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    public GoalDTO updateGoalStatus(Long goalId, String userEmail, Goal.Status newStatus) {
        User user = getUserByEmail(userEmail);
        Goal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goal.setStatus(newStatus);
        Goal updatedGoal = goalRepository.save(goal);
        return convertToDTO(updatedGoal);
    }

    public List<GoalDTO> getUserGoals(String userEmail) {
        User user = getUserByEmail(userEmail);
        return goalRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GoalDTO> getGoalsByStatus(String userEmail, Goal.Status status) {
        User user = getUserByEmail(userEmail);
        return goalRepository.findByUserIdAndStatus(user.getId(), status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<GoalDTO> getGoalsByAccount(String userEmail, Long accountId) {
        User user = getUserByEmail(userEmail);

        // Verify account belongs to user
        accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return goalRepository.findByUserIdAndAccountId(user.getId(), accountId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getGoalProgress(Long goalId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Goal goal = goalRepository.findByIdAndUserId(goalId, user.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        BigDecimal remaining = goal.getRemainingAmount();
        double progress = goal.getProgressPercentage();
        long daysRemaining = goal.getDaysRemaining();
        BigDecimal dailyRequired = goal.getDailyRequired();

        // Calculate achievement date based on current saving rate
        LocalDate estimatedCompletionDate = null;
        if (goal.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
            long daysElapsed = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now());
            if (daysElapsed > 0) {
                BigDecimal dailyRate = goal.getCurrentAmount().divide(
                        BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);

                if (dailyRate.compareTo(BigDecimal.ZERO) > 0) {
                    long estimatedDaysRemaining = remaining.divide(
                            dailyRate, 0, RoundingMode.UP).longValue();
                    estimatedCompletionDate = LocalDate.now().plusDays(estimatedDaysRemaining);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("goalId", goalId);
        result.put("goalName", goal.getName());
        result.put("targetAmount", goal.getTargetAmount());
        result.put("currentAmount", goal.getCurrentAmount());
        result.put("remainingAmount", remaining);
        result.put("progressPercentage", progress);
        result.put("status", goal.getStatus().name());
        result.put("targetDate", goal.getTargetDate());
        result.put("daysRemaining", daysRemaining);
        result.put("dailyRequired", dailyRequired);
        result.put("estimatedCompletionDate", estimatedCompletionDate);
        result.put("isOnTrack", isGoalOnTrack(goal));
        result.put("milestones", calculateMilestones(goal));
        return result;
    }

    public Map<String, Object> getGoalsOverview(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Goal> goals = goalRepository.findByUserId(user.getId());

        BigDecimal totalTarget = goals.stream()
                .map(Goal::getTargetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSaved = goals.stream()
                .map(Goal::getCurrentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = totalTarget.subtract(totalSaved);

        long inProgressCount = goals.stream()
                .filter(g -> g.getStatus() == Goal.Status.IN_PROGRESS)
                .count();

        long completedCount = goals.stream()
                .filter(g -> g.getStatus() == Goal.Status.COMPLETED)
                .count();

        long abandonedCount = goals.stream()
                .filter(g -> g.getStatus() == Goal.Status.ABANDONED)
                .count();

        // Get nearest deadline goal
        Goal nearestDeadline = goals.stream()
                .filter(g -> g.getStatus() == Goal.Status.IN_PROGRESS)
                .min((g1, g2) -> g1.getTargetDate().compareTo(g2.getTargetDate()))
                .orElse(null);

        // Get most funded goal
        Goal mostFunded = goals.stream()
                .filter(g -> g.getStatus() == Goal.Status.IN_PROGRESS)
                .max((g1, g2) -> g1.getProgressPercentage() > g2.getProgressPercentage() ? 1 : -1)
                .orElse(null);

        return Map.ofEntries(
                Map.entry("totalGoals", goals.size()),
                Map.entry("totalTargetAmount", totalTarget.setScale(2, RoundingMode.HALF_UP)),
                Map.entry("totalSavedAmount", totalSaved.setScale(2, RoundingMode.HALF_UP)),
                Map.entry("totalRemainingAmount", totalRemaining.setScale(2, RoundingMode.HALF_UP)),
                Map.entry("overallProgress", totalTarget.compareTo(BigDecimal.ZERO) == 0 ? 0.0 :
                        totalSaved.divide(totalTarget, 4, RoundingMode.HALF_UP).doubleValue() * 100),
                Map.entry("inProgressCount", inProgressCount),
                Map.entry("completedCount", completedCount),
                Map.entry("abandonedCount", abandonedCount),
                Map.entry("nearestDeadlineGoal", nearestDeadline != null ? convertToDTO(nearestDeadline) : null),
                Map.entry("mostFundedGoal", mostFunded != null ? convertToDTO(mostFunded) : null),
                Map.entry("goals", goals.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
        );
    }

    // ========== HELPER METHODS ==========

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private void updateGoalStatus(Goal goal) {
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(Goal.Status.COMPLETED);
        } else if (goal.getTargetDate().isBefore(LocalDate.now())) {
            goal.setStatus(Goal.Status.ABANDONED);
        } else {
            goal.setStatus(Goal.Status.IN_PROGRESS);
        }
    }

    private boolean isGoalOnTrack(Goal goal) {
        if (goal.getStatus() != Goal.Status.IN_PROGRESS) {
            return false;
        }

        long totalDays = ChronoUnit.DAYS.between(goal.getStartDate(), goal.getTargetDate());
        long daysElapsed = ChronoUnit.DAYS.between(goal.getStartDate(), LocalDate.now());

        if (totalDays <= 0 || daysElapsed <= 0) {
            return true;
        }

        double expectedProgress = (double) daysElapsed / totalDays * 100;
        double actualProgress = goal.getProgressPercentage();

        return actualProgress >= expectedProgress;
    }

    private Map<Integer, BigDecimal> calculateMilestones(Goal goal) {
        return Map.of(
                25, goal.getTargetAmount().multiply(BigDecimal.valueOf(0.25)),
                50, goal.getTargetAmount().multiply(BigDecimal.valueOf(0.50)),
                75, goal.getTargetAmount().multiply(BigDecimal.valueOf(0.75)),
                100, goal.getTargetAmount()
        );
    }

    private GoalDTO convertToDTO(Goal goal) {
        return new GoalDTO(
                goal.getId(),
                goal.getName(),
                goal.getDescription(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getTargetDate(),
                goal.getStartDate(),
                goal.getAccount() != null ? goal.getAccount().getId() : null,
                goal.getUser().getId(),
                goal.getStatus().name(),
                goal.getCreatedAt(),
                goal.getUpdatedAt(),
                goal.getIcon(),
                goal.getColor(),
                goal.getRemainingAmount(),
                goal.getProgressPercentage(),
                goal.getDaysRemaining(),
                goal.getDailyRequired()
        );
    }
}
