package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for processing loan approval/rejection decisions by administrators.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanApprovalDTO {

    @NotNull(message = "Loan ID is required")
    private Long loanId;

    @NotNull(message = "Approval status must be explicitly set")
    private Boolean approved;

    @DecimalMin(value = "1.0", message = "Interest rate must be at least 1%")
    @DecimalMax(value = "36.0", message = "Interest rate cannot exceed 36%")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal interestRate;

    /**
     * Mandatory for rejections, recommended for approvals for audit purposes.
     */
    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String adminRemarks;

    /**
     * Optional: Overriding the tenure if the admin suggests a different term
     * based on credit assessment.
     */
    private Integer approvedTenureMonths;
}