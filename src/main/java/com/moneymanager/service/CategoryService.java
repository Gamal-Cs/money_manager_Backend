// src/main/java/com/moneymanager/service/CategoryService.java
package com.moneymanager.service;

import com.moneymanager.dto.category.CategoryDTO;
import com.moneymanager.dto.category.CategoryRequest;
import com.moneymanager.model.Category;
import com.moneymanager.model.User;
import com.moneymanager.repository.CategoryRepository;
import com.moneymanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryDTO createCategory(String userEmail, CategoryRequest request) {
        User user = getUserByEmail(userEmail);

        // Validate type
        Category.Type type;
        try {
            type = Category.Type.valueOf(request.type().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Category type must be INCOME or EXPENSE");
        }

        // Check if category already exists for this user
        if (categoryRepository.existsByNameAndUserId(request.name(), user.getId())) {
            throw new RuntimeException("Category '" + request.name() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.name())
                .type(type)
                .user(user)
                .icon(request.icon())
                .description(request.description())
                .color(request.color())
                .build();

        // Set parent if provided
        if (request.parentCategoryId() != null) {
            Category parent = categoryRepository.findByIdAndUserId(
                            request.parentCategoryId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParentCategory(parent);
        }

        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }

    public List<CategoryDTO> getUserCategories(String userEmail) {
        User user = getUserByEmail(userEmail);
        return categoryRepository.findByUserId(user.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getCategoriesByType(String userEmail, Category.Type type) {
        User user = getUserByEmail(userEmail);
        return categoryRepository.findByUserIdAndType(user.getId(), type)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO updateCategory(Long categoryId, String userEmail, CategoryRequest request) {
        User user = getUserByEmail(userEmail);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Update fields
        category.setName(request.name());
        category.setType(Category.Type.valueOf(request.type().toUpperCase()));
        category.setIcon(request.icon());
        category.setDescription(request.description());
        category.setColor(request.color());

        // Update parent if changed
        if (request.parentCategoryId() != null) {
            Category parent = categoryRepository.findByIdAndUserId(
                            request.parentCategoryId(), user.getId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        Category updated = categoryRepository.save(category);
        return convertToDTO(updated);
    }

    public void deleteCategory(Long categoryId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check if category has transactions
        if (!category.getTransactions().isEmpty()) {
            throw new RuntimeException("Cannot delete category with existing transactions");
        }

        categoryRepository.delete(category);
    }

    public CategoryDTO getCategoryById(Long categoryId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return convertToDTO(category);
    }

    public List<CategoryDTO> getParentCategories(String userEmail) {
        User user = getUserByEmail(userEmail);
        return categoryRepository.findByUserIdAndParentCategoryIsNull(user.getId())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getSubcategories(Long parentId, String userEmail) {
        User user = getUserByEmail(userEmail);

        // Verify parent exists and belongs to user
        categoryRepository.findByIdAndUserId(parentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Parent category not found"));

        return categoryRepository.findByParentCategoryId(parentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    private CategoryDTO convertToDTO(Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getType().name(),
                category.getUser().getId(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null,
                category.getIcon(),
                category.getDescription(),
                category.getColor()
        );
    }
}