package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Data Transfer Object for processing loan repayments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanRepayDTO {

    @NotNull(message = "Loan ID is required")
    private Long loanId;

    @NotNull(message = "Repayment amount is required")
    @DecimalMin(value = "1.00", message = "Minimum repayment amount is ₹1.00")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @NotBlank(message = "Transaction PIN is required")
    @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
    @ToString.Exclude // Prevent sensitive PIN from being logged
    private String transactionPin;

    /**
     * Essential for preventing duplicate payments on network retry.
     */
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    /**
     * Optional: Indicates if this is a full foreclosure or a regular installment.
     */
    private boolean isFullForeclosure;
}