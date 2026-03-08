package com.revpay.service;

import com.revpay.exception.UnauthorizedException;
import com.revpay.model.dto.BusinessSummaryDTO;
import com.revpay.model.dto.RevenueReportDTO;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.Transaction.TransactionStatus;
import com.revpay.model.entity.Transaction.TransactionType;
import com.revpay.repository.TransactionAnalyticsRepository;
import com.revpay.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessAnalyticsService {

    private final TransactionAnalyticsRepository repository;

    private static final List<TransactionType> REVENUE_TYPES = List.of(
            TransactionType.SEND,
            TransactionType.INVOICE_PAYMENT
    );

    // ==============================
    // SUMMARY
    // ==============================

    public BusinessSummaryDTO getTransactionSummary(Long businessId) {
        log.info("ANALYTICS_SUMMARY | Request for Business ID: {}", businessId);
        validateBusinessAccess(businessId);

        List<Transaction> completed = repository.findByReceiverUserIdAndStatusAndTypeIn(
                businessId, TransactionStatus.COMPLETED, REVENUE_TYPES);

        List<Transaction> pending = repository.findByReceiverUserIdAndStatusAndTypeIn(
                businessId, TransactionStatus.PENDING, REVENUE_TYPES);

        BigDecimal totalReceived = sumAmounts(completed);
        BigDecimal pendingAmount = sumAmounts(pending);

        return BusinessSummaryDTO.builder()
                .totalReceived(totalReceived)
                .totalSent(BigDecimal.ZERO) // Placeholder for outgoing tracking
                .pendingAmount(pendingAmount)
                .totalTransactionCount(completed.size())
                .currency("INR")
                .build();
    }

    // ==============================
    // DAILY REVENUE
    // ==============================

    public List<RevenueReportDTO> getDailyRevenue(Long businessId) {
        log.info("ANALYTICS_DAILY | Request for Business ID: {}", businessId);
        validateBusinessAccess(businessId);

        List<Transaction> transactions = repository.findByReceiverUserIdAndStatusAndTypeIn(
                businessId, TransactionStatus.COMPLETED, REVENUE_TYPES);

        Map<LocalDate, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getTimestamp().toLocalDate()));

        List<RevenueReportDTO> reports = grouped.entrySet().stream()
                .map(entry -> buildRevenueDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RevenueReportDTO::getPeriod))
                .collect(Collectors.toList()); // Safer mutable list collection

        calculateGrowth(reports);
        return reports;
    }

    // ==============================
    // CUSTOM DATE RANGE
    // ==============================

    public List<RevenueReportDTO> getRevenueInDateRange(Long businessId, LocalDateTime start, LocalDateTime end) {
        log.info("ANALYTICS_RANGE | Business ID: {} | From: {} To: {}", businessId, start, end);
        validateBusinessAccess(businessId);

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and End date must be provided.");
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before End date.");
        }

        List<Transaction> transactions = repository.findByReceiverUserIdAndStatusAndTimestampBetween(
                businessId, TransactionStatus.COMPLETED, start, end);

        Map<LocalDate, List<Transaction>> grouped = transactions.stream()
                .filter(t -> REVENUE_TYPES.contains(t.getType()))
                .collect(Collectors.groupingBy(t -> t.getTimestamp().toLocalDate()));

        List<RevenueReportDTO> reports = grouped.entrySet().stream()
                .map(entry -> buildRevenueDTO(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(RevenueReportDTO::getPeriod))
                .collect(Collectors.toList());

        // FIXED: Growth was not being calculated for date ranges in the original code
        calculateGrowth(reports);

        return reports;
    }

    // ==============================
    // PRIVATE HELPERS
    // ==============================

    private BigDecimal sumAmounts(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RevenueReportDTO buildRevenueDTO(LocalDate date, List<Transaction> transactions) {
        return RevenueReportDTO.builder()
                .period(date.toString())
                .amount(sumAmounts(transactions))
                .transactionCount(transactions.size())
                .currency("INR")
                .growthPercentage(0.0) // Explicit default
                .build();
    }

    private void calculateGrowth(List<RevenueReportDTO> reports) {
        if (reports == null || reports.isEmpty()) return;

        for (int i = 1; i < reports.size(); i++) {
            BigDecimal current = reports.get(i).getAmount();
            BigDecimal previous = reports.get(i - 1).getAmount();

            if (previous.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal growth = current.subtract(previous)
                        .divide(previous, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                reports.get(i).setGrowthPercentage(growth.doubleValue());
            } else {
                reports.get(i).setGrowthPercentage(0.0);
            }
        }
    }

    private void validateBusinessAccess(Long businessId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Session invalid or unauthorized.");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) return; // Admins bypass strict ownership checks

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new UnauthorizedException("Invalid authentication token format.");
        }

        if (!userDetails.getUserId().equals(businessId)) {
            log.warn("UNAUTHORIZED_ACCESS_ATTEMPT | User {} attempted to view analytics for Business {}", userDetails.getUserId(), businessId);
            throw new UnauthorizedException("You do not have permission to view this business's analytics.");
        }
    }
}