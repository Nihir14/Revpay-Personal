package com.revpay.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_sender", columnList = "sender_id"),
        @Index(name = "idx_txn_receiver", columnList = "receiver_id"),
        @Index(name = "idx_txn_ref", columnList = "transaction_ref")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// REMOVED: @EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    @ToString.Exclude
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    @ToString.Exclude
    private User receiver;

    @NotNull(message = "Transaction amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    // FIXED: Switched to Hibernate's CreationTimestamp
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // FIXED: Switched to Hibernate's UpdateTimestamp
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "transaction_ref", unique = true, nullable = false, length = 50)
    private String transactionRef;

    public enum TransactionType {
        SEND, REQUEST, ADD_FUNDS, WITHDRAW, INVOICE_PAYMENT, LOAN_DISBURSEMENT, LOAN_REPAYMENT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, DECLINED, CANCELLED
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}