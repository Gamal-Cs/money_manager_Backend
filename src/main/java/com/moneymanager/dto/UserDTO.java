// src/main/java/com/moneymanager/dto/UserDTO.java
package com.moneymanager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be valid")
        String email,

        // Only for registration/update - not for response
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
        String phoneNumber,

        boolean active,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    // For registration (without timestamps)
    public static UserDTO forRegistration(
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        return new UserDTO(
                null,
                email,
                password,
                firstName,
                lastName,
                phoneNumber,
                true,
                null,
                null
        );
    }

    // For response (without password)
    public static UserDTO forResponse(
            Long id,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new UserDTO(
                id,
                email,
                null, // No password in response
                firstName,
                lastName,
                phoneNumber,
                active,
                createdAt,
                updatedAt
        );
    }
}

// Authentication DTOs


