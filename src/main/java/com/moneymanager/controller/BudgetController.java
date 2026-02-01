// src/main/java/com/moneymanager/controller/BudgetController.java
package com.moneymanager.controller;

import com.moneymanager.dto.ApiResponse;
import com.moneymanager.dto.budget.BudgetDTO;
import com.moneymanager.dto.budget.BudgetRequest;
import com.moneymanager.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    // ========== CRUD ENDPOINTS ==========

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetDTO>> createBudget(
            @Valid @RequestBody BudgetRequest request) {
        String userEmail = getCurrentUserEmail();
        BudgetDTO budget = budgetService.createBudget(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Budget created successfully", budget));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetDTO>> getBudgetById(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        BudgetDTO budget = budgetService.getBudgetById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Budget retrieved", budget));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetDTO>> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        String userEmail = getCurrentUserEmail();
        BudgetDTO updatedBudget = budgetService.updateBudget(id, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", updatedBudget));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        budgetService.deleteBudget(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully", null));
    }

    // ========== QUERY ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetDTO>>> getUserBudgets() {
        String userEmail = getCurrentUserEmail();
        List<BudgetDTO> budgets = budgetService.getUserBudgets(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Budgets retrieved", budgets));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BudgetDTO>>> getActiveBudgets() {
        String userEmail = getCurrentUserEmail();
        List<BudgetDTO> budgets = budgetService.getActiveBudgets(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Active budgets", budgets));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<BudgetDTO>>> getBudgetsByCategory(
            @PathVariable Long categoryId) {
        String userEmail = getCurrentUserEmail();
        List<BudgetDTO> budgets = budgetService.getBudgetsByCategory(userEmail, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category budgets", budgets));
    }

    // ========== ANALYSIS ENDPOINTS ==========

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBudgetProgress(
            @PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> progress = budgetService.getBudgetProgress(userEmail, id);
        return ResponseEntity.ok(ApiResponse.success("Budget progress analysis", progress));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBudgetOverview() {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> overview = budgetService.getBudgetOverview(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Budget overview", overview));
    }
}