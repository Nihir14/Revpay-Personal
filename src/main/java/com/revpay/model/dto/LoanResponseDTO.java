package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.revpay.model.entity.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Comprehensive response object for displaying loan details to users and admins.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {

    private Long loanId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal interestRate;

    private Integer tenureMonths;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal emiAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal remainingAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal totalPaid;

    private String purpose;
    private LoanStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;

    @Builder.Default
    private String currency = "INR";
}