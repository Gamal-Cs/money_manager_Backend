// src/main/java/com/moneymanager/controller/AnalyticsController.java
package com.moneymanager.controller;

import com.moneymanager.dto.ApiResponse;
import com.moneymanager.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardAnalytics() {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> analytics = analyticsService.getDashboardAnalytics(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Dashboard analytics", analytics));
    }

    @GetMapping("/monthly-trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyTrends(
            @RequestParam(defaultValue = "6") int months) {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> trends = analyticsService.getMonthlyTrends(userEmail, months);
        return ResponseEntity.ok(ApiResponse.success("Monthly trends", trends));
    }

    @GetMapping("/category-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCategoryAnalysis(
            @RequestParam(defaultValue = "MONTH") String period) {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> analysis = analyticsService.getCategoryAnalysis(userEmail, period);
        return ResponseEntity.ok(ApiResponse.success("Category analysis", analysis));
    }

    @GetMapping("/account-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountAnalysis() {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> analysis = analyticsService.getAccountAnalysis(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Account analysis", analysis));
    }

    @GetMapping("/financial-health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFinancialHealth() {
        String userEmail = getCurrentUserEmail();
        Map<String, Object> health = analyticsService.getFinancialHealth(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Financial health assessment", health));
    }
}