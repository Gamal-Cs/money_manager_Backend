// src/main/java/com/moneymanager/controller/CategoryController.java
package com.moneymanager.controller;

import com.moneymanager.dto.ApiResponse;
import com.moneymanager.dto.category.CategoryDTO;
import com.moneymanager.dto.category.CategoryRequest;
import com.moneymanager.model.Category;
import com.moneymanager.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return authentication.getName();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        String userEmail = getCurrentUserEmail();
        CategoryDTO category = categoryService.createCategory(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Category created", category));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getUserCategories() {
        String userEmail = getCurrentUserEmail();
        List<CategoryDTO> categories = categoryService.getUserCategories(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", categories));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategoriesByType(
            @PathVariable String type) {
        String userEmail = getCurrentUserEmail();
        Category.Type categoryType = Category.Type.valueOf(type.toUpperCase());
        List<CategoryDTO> categories = categoryService.getCategoriesByType(userEmail, categoryType);
        return ResponseEntity.ok(ApiResponse.success(
                type.toLowerCase() + " categories retrieved",
                categories
        ));
    }

    @GetMapping("/parents")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getParentCategories() {
        String userEmail = getCurrentUserEmail();
        List<CategoryDTO> categories = categoryService.getParentCategories(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Parent categories retrieved", categories));
    }

    @GetMapping("/{id}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getSubcategories(
            @PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        List<CategoryDTO> subcategories = categoryService.getSubcategories(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Subcategories retrieved", subcategories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        CategoryDTO category = categoryService.getCategoryById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        String userEmail = getCurrentUserEmail();
        CategoryDTO updatedCategory = categoryService.updateCategory(id, userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated", updatedCategory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        String userEmail = getCurrentUserEmail();
        categoryService.deleteCategory(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}