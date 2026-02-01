// src/main/java/com/moneymanager/controller/GoalController.java
package com.moneymanager.controller;

import com.moneymanager.dto.ApiResponse;
import com.moneymanager.dto.goal.GoalDTO;
import com.moneymanager.dto.goal.GoalRequest;
import com.moneymanager.model.Goal;
import com.moneymanager.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    // ========== CRUD ENDPOINTS ==========

    @PostMapping
    public ResponseEntity<ApiResponse<GoalDTO>> createGoal(
            @Valid @RequestBody GoalRequest request) {
        String userEmail = getCurrentUserEmail();
        GoalDTO goal = goalService.createGoal(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Goal created successfully", goal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalDTO>> getGoalById(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        GoalDTO goal = goalService.getGoalById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Goal retrieved", goal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalDTO>> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request) {
        String userEmail = getCurrentUserEmail();
        GoalDTO updatedGoal = goalService.updateGoal(id, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Goal updated successfully", updatedGoal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        goalService.deleteGoal(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Goal deleted successfully", null));
    }

    // ========== GOAL PROGRESS ENDPOINTS ==========

    @PostMapping("/{id}/add")
    public ResponseEntity<ApiResponse<GoalDTO>> addToGoal(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        String userEmail = getCurrentUserEmail();
        GoalDTO updatedGoal = goalService.addToGoal(id, userEmail, amount);
        return ResponseEntity.ok(ApiResponse.success("Amount added to goal", updatedGoal));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<GoalDTO>> updateGoalStatus(
            @PathVariable Long id,
            @RequestParam Goal.Status status) {
        String userEmail = getCurrentUserEmail();
        GoalDTO updatedGoal = goalService.updateGoalStatus(id, userEmail, status);
        return ResponseEntity.ok(ApiResponse.success("Goal status updated", updatedGoal));
    }

    // ========== QUERY ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalDTO>>> getUserGoals() {
        String userEmail = getCurrentUserEmail();
        List<GoalDTO> goals = goalService.getUserGoals(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Goals retrieved", goals));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<GoalDTO>>> getGoalsByStatus(
            @PathVariable Goal.Status status) {
        String userEmail = getCurrentUserEmail();
        List<GoalDTO> goals = goalService.getGoalsByStatus(userEmail, status);
        return ResponseEntity.ok(ApiResponse.success(status.name().toLowerCase() + " goals", goals));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<GoalDTO>>> getGoalsByAccount(
            @PathVariable Long accountId) {
        String userEmail = getCurrentUserEmail();
        List<GoalDTO> goals = goalService.getGoalsByAccount(userEmail, accountId);
        return ResponseEntity.ok(ApiResponse.success("Account goals", goals));
    }

    // ========== ANALYSIS ENDPOINTS ==========

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGoalProgress(
            @PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> progress = goalService.getGoalProgress(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Goal progress analysis", progress));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGoalsOverview() {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> overview = goalService.getGoalsOverview(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Goals overview", overview));
    }
}