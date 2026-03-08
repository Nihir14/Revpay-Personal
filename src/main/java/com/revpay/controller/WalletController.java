package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.dto.WalletAnalyticsDTO;
import com.revpay.model.entity.Transaction;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.WalletAnalyticsService;
import com.revpay.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Wallet Core", description = "Endpoints for managing wallet balance, funding, withdrawals, and direct transfers")
public class WalletController {

    private final WalletService walletService;
    private final WalletAnalyticsService analyticsService;

    public record TransactionDTO(Long transactionId, Long senderId, Long receiverId, BigDecimal amount, String type, String status, String description, LocalDateTime timestamp, String transactionRef) {}
    public record SimpleAmountRequest(@NotNull @Positive BigDecimal amount, String description) {}

    private Long getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    private TransactionDTO mapTx(Transaction t) {
        return new TransactionDTO(t.getTransactionId(), t.getSender() != null ? t.getSender().getUserId() : null, t.getReceiver() != null ? t.getReceiver().getUserId() : null, t.getAmount(), t.getType() != null ? t.getType().name() : null, t.getStatus() != null ? t.getStatus().name() : null, t.getDescription(), t.getTimestamp(), t.getTransactionRef());
    }

    @GetMapping("/balance")
    @Operation(summary = "Get wallet balance", description = "Retrieves the user's current wallet balance.")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getBalance(getUserId(auth)), "Balance retrieved"));
    }

    @PostMapping("/add-funds")
    @Operation(summary = "Add funds", description = "Simulates adding money to the digital wallet from a bank or card.")
    public ResponseEntity<ApiResponse<TransactionDTO>> addFunds(@Valid @RequestBody SimpleAmountRequest req, Authentication auth) {
        Transaction t = walletService.addFunds(getUserId(auth), req.amount(), req.description());
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Funds added"));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds", description = "Simulates withdrawing money from the digital wallet to a bank account.")
    public ResponseEntity<ApiResponse<TransactionDTO>> withdraw(@Valid @RequestBody SimpleAmountRequest req, Authentication auth) {
        Transaction t = walletService.withdrawFunds(getUserId(auth), req.amount());
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Withdrawal successful"));
    }

    @PostMapping("/send")
    @Operation(summary = "P2P Money Transfer", parameters = {@Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", required = true)})
    public ResponseEntity<ApiResponse<TransactionDTO>> transfer(@Valid @RequestBody TransactionRequest request, Authentication auth) {
        Transaction t = walletService.sendMoney(getUserId(auth), request);
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Transfer successful"));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get wallet analytics", description = "Retrieves spending analytics and category breakdowns for the user's wallet.")
    public ResponseEntity<ApiResponse<WalletAnalyticsDTO>> getAnalytics(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getSpendingAnalytics(getUserId(auth)), "Analytics calculated"));
    }
}