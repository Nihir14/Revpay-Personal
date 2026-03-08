package com.revpay.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request DTO for user self-registration.
 * Includes comprehensive validation and security exclusions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @ToString.Exclude // Prevent plain-text password logging
    private String password;

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "Transaction PIN must be 4 to 6 digits")
    @ToString.Exclude // Prevent sensitive PIN logging
    private String transactionPin;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(PERSONAL|BUSINESS)$", message = "Role must be either PERSONAL or BUSINESS")
    private String role;

    @NotBlank(message = "Security question is required")
    private String securityQuestion;

    @NotBlank(message = "Security answer is required")
    @ToString.Exclude // Prevent security answer logging
    private String securityAnswer;
}