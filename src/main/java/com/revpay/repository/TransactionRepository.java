package com.revpay.repository;

import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.Transaction.TransactionStatus;
import com.revpay.model.entity.Transaction.TransactionType;
import com.revpay.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver);

    Page<Transaction> findBySenderOrReceiverOrderByTimestampDesc(User sender, User receiver, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sender.userId = :userId OR t.receiver.userId = :userId) AND t.type = :type")
    Page<Transaction> findByUserAndType(@Param("userId") Long userId, @Param("type") TransactionType type, Pageable pageable);

    List<Transaction> findByReceiverUserIdAndTypeAndStatus(
            Long receiverUserId,
            TransactionType type,
            TransactionStatus status
    );

    Page<Transaction> findByReceiverUserIdAndTypeAndStatus(
            Long receiverUserId,
            TransactionType type,
            TransactionStatus status,
            Pageable pageable
    );

    List<Transaction> findBySenderUserIdAndTypeAndStatus(
            Long senderUserId,
            TransactionType type,
            TransactionStatus status
    );

    Page<Transaction> findBySenderUserIdAndTypeAndStatus(
            Long senderUserId,
            TransactionType type,
            TransactionStatus status,
            Pageable pageable
    );

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
}