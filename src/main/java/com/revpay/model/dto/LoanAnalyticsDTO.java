package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Provides a comprehensive snapshot of a user's or business's lending status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoanAnalyticsDTO {

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalOutstanding = BigDecimal.ZERO;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPending = BigDecimal.ZERO;

    // --- Added Analytics ---

    private int activeLoanCount;

    private int overdueInstallmentCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate nextPaymentDueDate;

    @Builder.Default
    private String currency = "INR";

    /**
     * Helper to determine if the account has any urgent issues.
     */
    public boolean hasOverduePayments() {
        return overdueInstallmentCount > 0;
    }
}