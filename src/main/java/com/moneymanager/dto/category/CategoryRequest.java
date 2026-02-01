package com.moneymanager.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String type,
        Long parentCategoryId,
        String icon,
        String description,
        String color
) {}