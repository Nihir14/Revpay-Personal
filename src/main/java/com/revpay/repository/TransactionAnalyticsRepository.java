package com.revpay.repository;

import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.Transaction.TransactionStatus;
import com.revpay.model.entity.Transaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionAnalyticsRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByReceiverUserIdAndStatusAndTypeIn(
            Long businessId,
            TransactionStatus status,
            List<TransactionType> types
    );

    List<Transaction> findByReceiverUserIdAndStatusAndTimestampBetween(
            Long businessId,
            TransactionStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    Page<Transaction> findByReceiverUserIdAndStatusAndTimestampBetween(
            Long businessId,
            TransactionStatus status,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.receiver.userId = :businessId " +
            "AND t.status = :status AND t.type IN :types")
    BigDecimal sumAmountByReceiverAndStatusAndTypeIn(
            @Param("businessId") Long businessId,
            @Param("status") TransactionStatus status,
            @Param("types") List<TransactionType> types
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.receiver.userId = :businessId " +
            "AND t.status = :status AND t.type IN :types")
    long countByReceiverAndStatusAndTypeIn(
            @Param("businessId") Long businessId,
            @Param("status") TransactionStatus status,
            @Param("types") List<TransactionType> types
    );
}