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
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    // FIXED: Added @Builder.Default so Lombok doesn't insert null
    @Builder.Default
    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    // FIXED: Added @Builder.Default
    @Builder.Default
    @Column(length = 3, nullable = false)
    private String currency = "INR";

    // FIXED: Switched to Hibernate's CreationTimestamp for guaranteed execution
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // FIXED: Switched to Hibernate's UpdateTimestamp
    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Version
    @Column(name = "version")
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return Objects.equals(userId, wallet.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}