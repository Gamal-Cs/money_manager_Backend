// src/main/java/com/moneymanager/controller/AccountController.java
package com.moneymanager.controller;

import com.moneymanager.dto.account.AccountDTO;
import com.moneymanager.dto.account.AccountRequest;
import com.moneymanager.dto.ApiResponse;
import com.moneymanager.service.AccountService;
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
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDTO>> createAccount(
            @Valid @RequestBody AccountRequest request) {
        String userEmail = getCurrentUserEmail();
        AccountDTO account = accountService.createAccount(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Account created", account));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDTO>>> getUserAccounts() {
        String userEmail = getCurrentUserEmail();
        List<AccountDTO> accounts = accountService.getUserAccounts(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved", accounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> getAccountById(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        AccountDTO account = accountService.getAccountById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        String userEmail = getCurrentUserEmail();
        AccountDTO updatedAccount = accountService.updateAccount(id, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Account updated", updatedAccount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        accountService.deleteAccount(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }

    @GetMapping("/total-balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalBalance() {
        String userEmail = getCurrentUserEmail();
        BigDecimal totalBalance = accountService.getTotalBalance(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Total balance", totalBalance));
    }

    @GetMapping("/balance-by-type")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getBalanceByAccountType() {
        String userEmail = getCurrentUserEmail();
        Map<String, BigDecimal> balanceByType = accountService.getBalanceByAccountType(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Balance by account type", balanceByType));
    }

    @PatchMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<AccountDTO>> updateAccountBalance(
            @PathVariable Long id,
            @RequestParam BigDecimal balance) {
        String userEmail = getCurrentUserEmail();
        AccountDTO updatedAccount = accountService.updateAccountBalance(id, userEmail, balance);
        return ResponseEntity.ok(ApiResponse.success("Account balance updated", updatedAccount));
    }
}