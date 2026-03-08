package com.revpay.service;

import com.revpay.model.dto.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Listener that handles post-transaction events asynchronously.
 * Using @TransactionalEventListener ensures notifications only fire IF the DB commit succeeds.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransferCompletedEvent(TransferCompletedEvent event) {
        log.info("ASYNC_EVENT_START | Processing notifications for TXN: {}", event.transactionId());

        try {
            // Formatting currency for better UX (e.g., ₹500.00)
            String formattedAmount = formatCurrency(event.amount(), event.currency());

            // 1. Notify Sender
            notificationService.createNotification(
                    event.senderId(),
                    String.format("Payment Successful: %s sent to %s. Ref: %s",
                            formattedAmount, event.receiverName(), event.transactionId()),
                    "TRANSFER"
            );

            // 2. Notify Receiver
            notificationService.createNotification(
                    event.receiverId(),
                    String.format("Money Received: You received %s from %s.",
                            formattedAmount, event.senderName()),
                    "TRANSFER"
            );

            log.info("ASYNC_EVENT_SUCCESS | Notifications dispatched for TXN: {}", event.transactionId());

        } catch (Exception e) {
            // Crucial: Async exceptions won't roll back the main transaction,
            // so we log them heavily for manual audit if needed.
            log.error("ASYNC_EVENT_FAILED | Failed to dispatch notifications for TXN {}. Error: {}",
                    event.transactionId(), e.getMessage(), e);
        }
    }

    /**
     * Helper to format the amount based on currency.
     */
    private String formatCurrency(java.math.BigDecimal amount, String currencyCode) {
        try {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            return formatter.format(amount);
        } catch (Exception e) {
            return currencyCode + " " + amount;
        }
    }
}