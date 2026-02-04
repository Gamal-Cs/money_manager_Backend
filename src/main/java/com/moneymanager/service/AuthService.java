// src/main/java/com/moneymanager/service/AuthService.java
package com.moneymanager.service;

import com.moneymanager.dto.UserDTO;
import com.moneymanager.dto.auth.AuthResponse;
import com.moneymanager.dto.auth.LoginRequest;
import com.moneymanager.dto.auth.RegisterRequest;
import com.moneymanager.model.User;
import com.moneymanager.repository.UserRepository;
import com.moneymanager.security.CustomUserDetails;
import com.moneymanager.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(
                new CustomUserDetails(
                        savedUser
                )
        );
        return new AuthResponse(token, convertToUserDTO(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Load user details
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Generate token
        String token = jwtUtil.generateToken(
                new CustomUserDetails(
                        user
                )
        );

        return new AuthResponse(token, convertToUserDTO(user));
    }

    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return convertToUserDTO(user);
    }

    private UserDTO convertToUserDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                null, // Don't expose password
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
