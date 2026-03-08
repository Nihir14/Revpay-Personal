package com.revpay.repository;

import com.revpay.model.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, String> {

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}