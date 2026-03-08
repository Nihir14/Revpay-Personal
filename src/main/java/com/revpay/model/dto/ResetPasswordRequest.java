package com.revpay.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for finalizing the password reset process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Security answer is required")
    @ToString.Exclude // Prevent security answer from being logged
    private String securityAnswer;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    @ToString.Exclude // Prevent plain-text password from being logged
    private String newPassword;

    /**
     * Optional: Reset token if the system uses email-link or OTP-based resets.
     */
    private String resetToken;
}