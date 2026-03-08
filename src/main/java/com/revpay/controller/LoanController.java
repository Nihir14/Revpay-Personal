package com.revpay.controller;

import com.revpay.model.dto.*;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.CreditScoringService;
import com.revpay.service.LoanOperationService;
import com.revpay.service.LoanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'BUSINESS')")
@Tag(name = "User Loan Operations", description = "Endpoints for users to apply for, manage, and repay loans, as well as view credit analytics")
public class LoanController {

    private final LoanOperationService operationService;
    private final LoanQueryService queryService;
    private final CreditScoringService scoringService;

    // --- LOCAL DTOs (Restored to fix compilation errors) ---
    public record LoanDTO(Long loanId, Long userId, BigDecimal amount, BigDecimal interestRate, Integer tenureMonths, BigDecimal emiAmount, BigDecimal remainingAmount, String purpose, String status, LocalDate startDate, LocalDate endDate) {}
    public record InstallmentDTO(Long installmentId, Long loanId, Integer installmentNumber, BigDecimal amount, LocalDate dueDate, String status) {}

    // --- HELPER METHOD (Restored to fix getUserId() errors) ---
    private Long getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    // --- ENDPOINTS ---

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new loan", description = "Submits a new loan application for the authenticated user.")
    public ResponseEntity<ApiResponse<LoanResponseDTO>> applyLoan(@Valid @RequestBody LoanApplyDTO dto, Authentication auth) {
        log.info("User ID: {} is applying for a loan", getUserId(auth));
        return ResponseEntity.ok(ApiResponse.success(operationService.applyLoan(getUserId(auth), dto), "Loan application submitted"));
    }

    @PostMapping("/repay")
    @Operation(summary = "Repay loan installment", description = "Processes a repayment towards an active loan's EMI or outstanding balance.")
    public ResponseEntity<ApiResponse<String>> repayLoan(@Valid @RequestBody LoanRepayDTO dto, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(operationService.repayLoan(getUserId(auth), dto), "Loan repayment processed"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my loans", description = "Retrieves a paginated list of all loans belonging to the authenticated user.")
    public ResponseEntity<ApiResponse<Page<LoanDTO>>> myLoans(Authentication auth, @PageableDefault(size = 10) Pageable pageable) {
        Page<LoanDTO> dtos = queryService.getUserLoansPaged(getUserId(auth), pageable).map(l -> new LoanDTO(
                l.getLoanId(), l.getUser().getUserId(), l.getAmount(), l.getInterestRate(),
                l.getTenureMonths(), l.getEmiAmount(), l.getRemainingAmount(), l.getPurpose(),
                l.getStatus().name(), l.getStartDate(), l.getEndDate()
        ));
        return ResponseEntity.ok(ApiResponse.success(dtos, "User loans retrieved"));
    }

    @GetMapping("/outstanding")
    @Operation(summary = "Get total outstanding amount", description = "Calculates the total remaining amount across all active loans.")
    public ResponseEntity<ApiResponse<BigDecimal>> totalOutstanding(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(queryService.totalOutstanding(getUserId(auth)), "Total outstanding retrieved"));
    }

    @GetMapping("/emi/{loanId}")
    @Operation(summary = "View EMI schedule", description = "Retrieves the paginated EMI schedule for a specific loan.")
    public ResponseEntity<ApiResponse<Page<InstallmentDTO>>> viewEmiSchedule(
            @Parameter(description = "ID of the loan") @PathVariable Long loanId,
            Authentication auth,
            @PageableDefault(size = 12) Pageable pageable) {

        Page<InstallmentDTO> dtos = queryService.getEmiSchedulePaged(getUserId(auth), loanId, pageable).map(i -> new InstallmentDTO(
                i.getInstallmentId(), i.getLoan().getLoanId(), i.getInstallmentNumber(),
                i.getAmount(), i.getDueDate(), i.getStatus().name()
        ));
        return ResponseEntity.ok(ApiResponse.success(dtos, "EMI schedule retrieved"));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue EMIs", description = "Retrieves a paginated list of all overdue EMI installments.")
    public ResponseEntity<ApiResponse<Page<InstallmentDTO>>> getOverdues(Authentication auth, @PageableDefault(size = 10) Pageable pageable) {
        Page<InstallmentDTO> dtos = queryService.getOverdueEmisPaged(getUserId(auth), pageable).map(i -> new InstallmentDTO(
                i.getInstallmentId(), i.getLoan().getLoanId(), i.getInstallmentNumber(),
                i.getAmount(), i.getDueDate(), i.getStatus().name()
        ));
        return ResponseEntity.ok(ApiResponse.success(dtos, "Overdue EMIs retrieved"));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get loan analytics", description = "Retrieves total outstanding, paid, and pending loan amounts.")
    public ResponseEntity<ApiResponse<LoanAnalyticsDTO>> getAnalytics(Authentication auth) {
        Long userId = getUserId(auth);
        LoanAnalyticsDTO dto = LoanAnalyticsDTO.builder()
                .totalOutstanding(queryService.totalOutstanding(userId))
                .totalPaid(queryService.totalPaid(userId))
                .totalPending(queryService.totalPending(userId))
                .build();
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan analytics retrieved"));
    }

    @PostMapping("/preclose/{loanId}")
    @Operation(summary = "Pre-close loan", description = "Initiates loan pre-closure, calculating any pre-closure charges.")
    public ResponseEntity<ApiResponse<String>> preCloseLoan(
            @Parameter(description = "ID of the loan to pre-close") @PathVariable Long loanId,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(operationService.preCloseLoan(getUserId(auth), loanId), "Pre-closure processed"));
    }

    @GetMapping("/credit-score")
    @Operation(summary = "Get user credit score", description = "Calculates the user's internal platform credit score.")
    public ResponseEntity<ApiResponse<Integer>> getCreditScore(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(scoringService.calculateCreditScore(getUserId(auth)), "Credit score retrieved"));
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Check loan eligibility", description = "Evaluates the user's financial profile for loan constraints.")
    public ResponseEntity<ApiResponse<LoanEligibilityDTO>> checkEligibility(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(scoringService.checkEligibility(getUserId(auth)), "Eligibility retrieved"));
    }

    @GetMapping("/recommendation")
    @Operation(summary = "Get loan recommendation", description = "Provides AI-driven loan recommendations based on credit score.")
    public ResponseEntity<ApiResponse<LoanRecommendationDTO>> getRecommendation(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(scoringService.getLoanRecommendation(getUserId(auth)), "Recommendation retrieved"));
    }
}