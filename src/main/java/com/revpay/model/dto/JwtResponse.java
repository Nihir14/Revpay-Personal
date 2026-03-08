package com.revpay.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO returned upon successful authentication,
 * containing the JWT and essential user profile data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private String refreshToken;

    private Long userId;
    private String email;
    private String fullName;

    /**
     * Supports multiple roles if the system evolves,
     * but remains compatible with single roles.
     */
    private List<String> roles;

    /**
     * Token validity duration in milliseconds.
     */
    private Long expiresIn;

    // Custom constructor for backward compatibility with existing AuthService logic
    public JwtResponse(String token, Long userId, String email, String role) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.email = email;
        this.roles = List.of(role);
    }
}