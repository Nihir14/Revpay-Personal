package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for new loan applications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplyDTO {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is ₹1,000")
    @DecimalMax(value = "1000000.00", message = "Maximum loan amount is ₹10,00,000")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @NotNull(message = "Tenure is required")
    @Min(value = 3, message = "Minimum tenure is 3 months")
    @Max(value = 60, message = "Maximum tenure is 60 months")
    private Integer tenureMonths;

    @NotBlank(message = "Loan purpose is required")
    @Size(max = 255, message = "Purpose must not exceed 255 characters")
    private String purpose;

    /**
     * Categorizes the loan for risk assessment (e.g., WORKING_CAPITAL, EXPANSION).
     */
    @NotBlank(message = "Loan type is required")
    private String loanType;

    /**
     * Essential for fintech reliability to prevent duplicate applications.
     */
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @Builder.Default
    private String currency = "INR";
}