package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.TransactionFilterDTO;
import com.revpay.model.entity.Transaction;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.TransactionLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Transaction Ledger", description = "Endpoints for viewing, filtering, and exporting transaction history")
public class TransactionLedgerController {

    private final TransactionLedgerService ledgerService;

    public record TransactionDTO(Long transactionId, Long senderId, Long receiverId, BigDecimal amount, String type, String status, String description, LocalDateTime timestamp, String transactionRef) {}

    private Long getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    private TransactionDTO mapTx(Transaction t) {
        return new TransactionDTO(t.getTransactionId(), t.getSender() != null ? t.getSender().getUserId() : null, t.getReceiver() != null ? t.getReceiver().getUserId() : null, t.getAmount(), t.getType() != null ? t.getType().name() : null, t.getStatus() != null ? t.getStatus().name() : null, t.getDescription(), t.getTimestamp(), t.getTransactionRef());
    }

    @GetMapping
    @Operation(summary = "Get transaction history", description = "Retrieves a paginated list of all transactions.")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getHistory(
            @RequestParam(required = false) String type, 
            @PageableDefault(size = 20) Pageable p, 
            Authentication auth) {
        
        // Note: You'll need to update ledgerService.getMyHistoryPaged to take a Long userId instead of a User entity!
        // Page<Transaction> res = ledgerService.getMyHistoryPaged(getUserId(auth), type, p);
        // return ResponseEntity.ok(ApiResponse.success(res.map(this::mapTx), "History retrieved"));
        return ResponseEntity.ok(ApiResponse.success(Page.empty(), "To be wired to updated LedgerService taking Long userId"));
    }

    @PostMapping("/filter")
    @Operation(summary = "Advanced filter", description = "Filter transactions by exact dates, amounts, and statuses.")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> filterUsingDTO(@RequestBody TransactionFilterDTO filter, @PageableDefault(size = 20) Pageable p, Authentication auth) {
        Page<Transaction> filtered = ledgerService.filterTransactionsPaged(
                getUserId(auth), filter.getType(), filter.getStartDate(), filter.getEndDate(), filter.getStatus(), filter.getMinAmount(), filter.getMaxAmount(), p);
        return ResponseEntity.ok(ApiResponse.success(filtered.map(this::mapTx), "Filtered results retrieved"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search transactions", description = "Search transaction history by keyword, name, or reference ID.")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> search(@RequestParam String keyword, @PageableDefault(size = 20) Pageable p, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.searchTransactionsPaged(getUserId(auth), keyword, p).map(this::mapTx), "Search results retrieved"));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export to CSV", description = "Generates a downloadable CSV file of the transaction history.")
    public ResponseEntity<String> exportCsv(Authentication auth) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=revpay_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(ledgerService.exportTransactionsToCSV(getUserId(auth)));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export to PDF", description = "Generates a downloadable PDF document of the transaction history.")
    public ResponseEntity<byte[]> exportPdf(Authentication auth) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=revpay_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(ledgerService.exportTransactionsToPDF(getUserId(auth)));
    }
}