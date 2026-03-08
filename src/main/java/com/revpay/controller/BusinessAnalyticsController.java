package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.BusinessSummaryDTO;
import com.revpay.model.dto.RevenueReportDTO;
import com.revpay.service.BusinessAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/analytics") // RESTful routing fix!
@RequiredArgsConstructor
@Tag(name = "Business Analytics", description = "Endpoints for generating financial reports and transaction summaries for business accounts")
public class BusinessAnalyticsController {

    private final BusinessAnalyticsService analyticsService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
    @Operation(summary = "Get business transaction summary", description = "Retrieves a high-level overview of transaction volumes and revenue.")
    public ResponseEntity<ApiResponse<BusinessSummaryDTO>> getSummary(
            @Parameter(description = "ID of the business profile") @PathVariable Long businessId) {
        log.debug("Fetching transaction summary for business ID: {}", businessId);
        BusinessSummaryDTO summary = analyticsService.getTransactionSummary(businessId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Business summary retrieved successfully"));
    }

    @GetMapping("/revenue/daily")
    @PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
    @Operation(summary = "Get daily revenue report", description = "Generates a day-by-day breakdown of revenue.")
    public ResponseEntity<ApiResponse<List<RevenueReportDTO>>> getDailyRevenue(
            @Parameter(description = "ID of the business profile") @PathVariable Long businessId) {
        log.debug("Fetching daily revenue report for business ID: {}", businessId);
        List<RevenueReportDTO> revenue = analyticsService.getDailyRevenue(businessId);
        return ResponseEntity.ok(ApiResponse.success(revenue, "Daily revenue retrieved successfully"));
    }

    @GetMapping("/revenue/range")
    @PreAuthorize("hasAnyRole('BUSINESS', 'ADMIN')")
    @Operation(summary = "Get revenue by date range", description = "Generates a revenue report for a custom specified date range.")
    public ResponseEntity<ApiResponse<List<RevenueReportDTO>>> getRevenueInDateRange(
            @Parameter(description = "ID of the business profile") @PathVariable Long businessId,
            @Parameter(description = "Start date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "End date (ISO format)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("Fetching custom revenue report for business ID: {} | Range: {} to {}", businessId, start, end);
        List<RevenueReportDTO> revenue = analyticsService.getRevenueInDateRange(businessId, start, end);
        return ResponseEntity.ok(ApiResponse.success(revenue, "Date range revenue retrieved successfully"));
    }
}