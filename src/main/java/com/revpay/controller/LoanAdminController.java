package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.LoanApprovalDTO;
import com.revpay.model.entity.Loan;
import com.revpay.service.LoanOperationService;
import com.revpay.service.LoanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/loans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Hoisted to class level for global enforcement
@Tag(name = "Admin Loan Operations", description = "Endpoints for administrators to manage, review, and approve user loans")
public class LoanAdminController {

    // Injecting the newly split services!
    private final LoanOperationService operationService;
    private final LoanQueryService queryService;

    // --- DTOs ---
    public record LoanDTO(Long loanId, Long userId, BigDecimal amount, BigDecimal interestRate, Integer tenureMonths, BigDecimal emiAmount, BigDecimal remainingAmount, String purpose, String status, LocalDate startDate, LocalDate endDate) {}

    // --- ENDPOINTS ---

    @PostMapping("/approve")
    @Operation(summary = "Approve or Reject Loan", description = "Processes a loan application based on admin decision. If approved, funds are automatically disbursed to the user's wallet.")
    public ResponseEntity<ApiResponse<String>> approveLoan(@Valid @RequestBody LoanApprovalDTO dto) {
        log.info("Admin initiated loan approval process for Loan ID: {}", dto.getLoanId());

        // Routed to LoanOperationService
        String result = operationService.approveLoan(dto);

        log.info("Loan approval process completed for Loan ID: {}. Result: {}", dto.getLoanId(), result);
        return ResponseEntity.ok(ApiResponse.success(result, "Loan approval process completed"));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all platform loans", description = "Retrieves a paginated list of all loans across the RevPay platform for administrative oversight.")
    public ResponseEntity<ApiResponse<Page<LoanDTO>>> getAllLoans(@PageableDefault(size = 10) Pageable pageable) {
        log.debug("Admin requested all system loans. Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());

        // Routed to LoanQueryService
        Page<Loan> loans = queryService.getAllLoansPaged(pageable);

        Page<LoanDTO> dtos = loans.map(l -> new LoanDTO(
                l.getLoanId(),
                l.getUser().getUserId(),
                l.getAmount(),
                l.getInterestRate(),
                l.getTenureMonths(),
                l.getEmiAmount(),
                l.getRemainingAmount(),
                l.getPurpose(),
                l.getStatus().name(),
                l.getStartDate(),
                l.getEndDate()
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "All system loans retrieved successfully"));
    }
}