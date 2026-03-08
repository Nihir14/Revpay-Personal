package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revpay.model.entity.RiskTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object providing a pre-application assessment of a user's creditworthiness.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanEligibilityDTO {

    private int creditScore;

    private RiskTier riskTier;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal maxEligibleAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal applicableInterestRate;

    private int maxTenureMonths;

    private boolean eligible;

    /**
     * Descriptive message for the user (e.g., "Congratulations! You are eligible for a premium rate.")
     */
    private String message;

    @Builder.Default
    private String currency = "INR";
}