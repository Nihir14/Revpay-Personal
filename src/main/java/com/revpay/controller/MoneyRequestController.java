package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.Transaction;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.MoneyRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Money Requests", description = "Endpoints for initiating, accepting, and managing P2P money requests")
public class MoneyRequestController {

    private final MoneyRequestService requestService;

    public record TransactionDTO(Long transactionId, Long senderId, Long receiverId, BigDecimal amount, String type, String status, String description, LocalDateTime timestamp, String transactionRef) {}
    public record PinPayload(@NotBlank String pin) {}
    public record MoneyRequestPayload(@NotBlank String targetEmail, @NotNull @Positive BigDecimal amount) {}

    private Long getUserId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    private TransactionDTO mapTx(Transaction t) {
        return new TransactionDTO(t.getTransactionId(), t.getSender() != null ? t.getSender().getUserId() : null, t.getReceiver() != null ? t.getReceiver().getUserId() : null, t.getAmount(), t.getType() != null ? t.getType().name() : null, t.getStatus() != null ? t.getStatus().name() : null, t.getDescription(), t.getTimestamp(), t.getTransactionRef());
    }

    @PostMapping
    @Operation(summary = "Request money", description = "Initiates a new money request to another user.")
    public ResponseEntity<ApiResponse<TransactionDTO>> requestMoney(@Valid @RequestBody MoneyRequestPayload req, Authentication auth) {
        Transaction t = requestService.requestMoney(getUserId(auth), req.targetEmail(), req.amount());
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Request initiated"));
    }

    @PostMapping("/{txnId}/accept")
    @Operation(summary = "Accept money request", description = "Fulfills a pending request and transfers the funds.")
    public ResponseEntity<ApiResponse<TransactionDTO>> acceptRequest(@PathVariable Long txnId, @Valid @RequestBody PinPayload body) {
        Transaction t = requestService.acceptRequest(txnId, body.pin());
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Request fulfilled"));
    }

    @PostMapping("/{txnId}/decline")
    @Operation(summary = "Decline money request", description = "Rejects an incoming money request.")
    public ResponseEntity<ApiResponse<TransactionDTO>> declineRequest(@PathVariable Long txnId, Authentication auth) {
        Transaction t = requestService.declineRequest(getUserId(auth), txnId);
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Request declined"));
    }

    @PostMapping("/{txnId}/cancel")
    @Operation(summary = "Cancel outgoing request", description = "Cancels a money request that you previously sent to someone else.")
    public ResponseEntity<ApiResponse<TransactionDTO>> cancelOutgoingRequest(@PathVariable Long txnId, Authentication auth) {
        Transaction t = requestService.cancelOutgoingRequest(getUserId(auth), txnId);
        return ResponseEntity.ok(ApiResponse.success(mapTx(t), "Request cancelled"));
    }

    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getIncoming(@PageableDefault(size = 10) Pageable p, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getIncomingRequestsPaged(getUserId(auth), p).map(this::mapTx), "Incoming requests fetched"));
    }

    @GetMapping("/outgoing")
    public ResponseEntity<ApiResponse<Page<TransactionDTO>>> getOutgoing(@PageableDefault(size = 10) Pageable p, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getOutgoingRequestsPaged(getUserId(auth), p).map(this::mapTx), "Outgoing requests fetched"));
    }
}