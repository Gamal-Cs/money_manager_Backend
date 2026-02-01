package com.moneymanager.dto.auth;

import com.moneymanager.dto.UserDTO;

public record AuthResponse(
        String token,
        UserDTO user
) {}