package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published immediately after a successful wallet transfer.
 * Note: @Builder.Default is not supported for records.
 * Defaults are handled in the canonical constructor.
 */
@Builder
public record TransferCompletedEvent(
        Long transactionId,
        Long senderId,
        Long receiverId,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        BigDecimal amount,

        String senderName,
        String receiverName,
        String currency,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {
    /**
     * Canonical Constructor: The single point of truth for object creation.
     * We apply defaults here to handle Builder, manual calls, and deserialization.
     */
    public TransferCompletedEvent {
        amount = (amount == null) ? BigDecimal.ZERO : amount;
        currency = (currency == null) ? "INR" : currency;
        timestamp = (timestamp == null) ? LocalDateTime.now() : timestamp;
    }
}