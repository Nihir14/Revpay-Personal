package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for filtering and searching through transaction history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterDTO {

    private String type; // e.g., SEND, ADD_FUNDS, LOAN_REPAYMENT

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    private String status; // e.g., COMPLETED, PENDING, FAILED

    @DecimalMin(value = "0.0", message = "Minimum amount cannot be negative")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal minAmount;

    @DecimalMin(value = "0.0", message = "Maximum amount cannot be negative")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal maxAmount;

    /**
     * Pagination parameters to ensure optimized database queries.
     */
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    private String sortBy = "timestamp";

    private String direction = "DESC";
}