package com.revpay.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for processing an invoice payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePaymentRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    @NotBlank(message = "Transaction PIN is required")
    @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 digits")
    @ToString.Exclude // Prevent PIN from being logged
    private String transactionPin;

    /**
     * Optional: Client-side amount to verify against the server-side invoice total.
     * Prevents payment if the invoice was modified recently.
     */
    private BigDecimal amount;

    /**
     * Required for fintech safety to prevent duplicate charges on retry.
     */
    @NotBlank(message = "Idempotency key is required for payments")
    private String idempotencyKey;
}