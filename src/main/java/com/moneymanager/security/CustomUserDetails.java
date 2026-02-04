package com.moneymanager.security;

import com.moneymanager.model.User; // Update import
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // If you have roles in your User entity, add them here
        // For now, returning empty list since your User entity doesn't have roles
        return List.of();

        // If you add roles later, uncomment and modify this:
        /*
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());
        */
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // Using email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.isActive(); // Using active status for account expiration
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isActive(); // Using active status for account lock
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.isActive(); // Using active status for credentials
    }

    @Override
    public boolean isEnabled() {
        return user.isActive(); // Using active status for enabled
    }
}