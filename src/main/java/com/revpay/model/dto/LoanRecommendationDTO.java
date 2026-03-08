package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revpay.model.entity.RiskTier;
import com.revpay.model.entity.VipTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for AI-driven or rule-based loan recommendations
 * tailored to a specific user's financial profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRecommendationDTO {

    private int creditScore;
    private RiskTier riskTier;
    private VipTier vipTier;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal recommendedAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal expectedInterestRate;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal estimatedEmi;

    private int recommendedTenureMonths;

    /**
     * Explains the basis of the recommendation (e.g., "High credit score bonus applied").
     */
    private String logicReasoning;

    @Builder.Default
    private String currency = "INR";
}