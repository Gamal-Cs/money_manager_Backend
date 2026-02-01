// src/main/java/com/moneymanager/controller/TransactionController.java
package com.moneymanager.controller;

import com.moneymanager.dto.ApiResponse;
import com.moneymanager.dto.transaction.TransactionDTO;
import com.moneymanager.dto.transaction.TransactionRequest;
import com.moneymanager.dto.transaction.TransactionSummary;
import com.moneymanager.model.Transaction;
import com.moneymanager.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    // Helper method to get current user email
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    // ========== CRUD ENDPOINTS ==========

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDTO>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        String userEmail = getCurrentUserEmail();
        TransactionDTO created = transactionService.createTransaction(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDTO>> getTransactionById(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        TransactionDTO transaction = transactionService.getTransactionById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved", transaction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDTO>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        String userEmail = getCurrentUserEmail();
        TransactionDTO updated = transactionService.updateTransaction(id, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        transactionService.deleteTransaction(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }

    // ========== QUERY ENDPOINTS ==========

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getUserTransactions() {
        String userEmail = getCurrentUserEmail();
        List<TransactionDTO> transactions = transactionService.getTransactionsByUser(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", transactions));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        String userEmail = getCurrentUserEmail();
        List<TransactionDTO> transactions = transactionService.getRecentTransactions(userEmail, limit);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Last %d transactions", limit),
                transactions
        ));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getFilteredTransactions(
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String userEmail = getCurrentUserEmail();
        List<TransactionDTO> transactions = transactionService
                .getTransactionsWithFilters(userEmail, type, startDate, endDate, null, null);

        return ResponseEntity.ok(ApiResponse.success("Filtered transactions retrieved", transactions));
    }

    @GetMapping("/advanced-filter")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getAdvancedFilteredTransactions(
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId) {

        String userEmail = getCurrentUserEmail();
        List<TransactionDTO> transactions = transactionService.getTransactionsWithFilters(
                userEmail, type, startDate, endDate, accountId, categoryId);

        return ResponseEntity.ok(ApiResponse.success("Advanced filtered transactions", transactions));
    }

    // ========== ACCOUNT-RELATED ENDPOINTS ==========

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByAccount(
            @PathVariable Long accountId) {
        String userEmail = getCurrentUserEmail();
        List<TransactionDTO> transactions = transactionService.getTransactionsByAccount(userEmail, accountId);
        return ResponseEntity.ok(ApiResponse.success("Account transactions", transactions));
    }

    @GetMapping("/account/{accountId}/balance-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountBalanceSummary(
            @PathVariable Long accountId) {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> summary = transactionService.getAccountBalanceSummary(userEmail, accountId);
        return ResponseEntity.ok(ApiResponse.success("Account balance summary", summary));
    }

    // ========== CATEGORY-RELATED ENDPOINTS ==========

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<TransactionDTO>>> getTransactionsByCategory(
            @PathVariable Long categoryId) {
        String userEmail = getCurrentUserEmail();
        List<TransactionDTO> transactions = transactionService.getTransactionsByCategory(userEmail, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category transactions", transactions));
    }

    @GetMapping("/category/{categoryId}/spending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategorySpending(
            @PathVariable Long categoryId) {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> spending = transactionService.getCategorySpending(userEmail, categoryId);
        return ResponseEntity.ok(ApiResponse.success("Category spending analysis", spending));
    }

    // ========== BALANCE ENDPOINTS ==========

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance() {
        String userEmail = getCurrentUserEmail();
        BigDecimal balance = transactionService.getBalance(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Current balance", balance));
    }

    @GetMapping("/balance/details")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getBalanceDetails() {
        String userEmail = getCurrentUserEmail();
        Map<String, BigDecimal> balanceDetails = transactionService.getBalanceDetails(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Balance details", balanceDetails));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransactionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String userEmail = getCurrentUserEmail();
        TransactionSummary summary = transactionService.getTransactionSummary(userEmail, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Transaction summary", summary));
    }
}