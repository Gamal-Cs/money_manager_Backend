// src/main/java/com/moneymanager/dto/CategoryDTO.java
package com.moneymanager.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryDTO(
        Long id,

        @NotBlank(message = "Category name is required")
        String name,

        @NotBlank(message = "Category type is required")
        String type, // "INCOME" or "EXPENSE"

        Long userId,

        Long parentCategoryId,

        String icon,

        String description,

        String color
) {}
