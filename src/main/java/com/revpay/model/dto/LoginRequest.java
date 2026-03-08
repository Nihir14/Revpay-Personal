package com.revpay.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for user authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @ToString.Exclude // Crucial security measure for logging
    private String password;

    /**
     * Optional: Client identifier to track login origin (e.g., "Web", "Mobile-iOS").
     */
    private String clientSource;
}