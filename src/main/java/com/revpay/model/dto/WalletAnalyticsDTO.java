package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Data Transfer Object providing deep insights into a user's wallet activity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WalletAnalyticsDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal currentBalance;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalSpent;

    private Map<String, BigDecimal> spendingByCategory;

    private Long transactionCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal averageTransactionValue;

    private Double monthlyChangePercentage;

    @Builder.Default
    private String currency = "INR";
}