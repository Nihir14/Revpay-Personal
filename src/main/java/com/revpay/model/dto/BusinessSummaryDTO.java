package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Provides a high-level financial snapshot for business accounts.
 */
@Builder
public record BusinessSummaryDTO(

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalReceived,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal totalSent,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal pendingAmount,

        long totalTransactionCount,

        String currency
) {
    // Canonical constructor to ensure non-null values
    public BusinessSummaryDTO {
        totalReceived = (totalReceived == null) ? BigDecimal.ZERO : totalReceived;
        totalSent = (totalSent == null) ? BigDecimal.ZERO : totalSent;
        pendingAmount = (pendingAmount == null) ? BigDecimal.ZERO : pendingAmount;
        currency = (currency == null) ? "INR" : currency;
    }
}