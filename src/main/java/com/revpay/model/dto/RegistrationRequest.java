package com.revpay.model.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for new user registration.
 * Handles both Personal and Business account types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @ToString.Exclude
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(PERSONAL|BUSINESS)$", message = "Role must be PERSONAL or BUSINESS")
    private String role;

    @NotBlank(message = "Transaction PIN is required")
    @Pattern(regexp = "^[0-9]{4,6}$", message = "PIN must be 4-6 digits")
    @ToString.Exclude
    private String transactionPin;

    @NotBlank(message = "Security question is required")
    private String securityQuestion;

    @NotBlank(message = "Security answer is required")
    @ToString.Exclude
    private String securityAnswer;

    // Optional Business Fields
    @Size(max = 150, message = "Business name too long")
    private String businessName;

    private String businessType; // e.g., RETAIL, SERVICES, TECH
}