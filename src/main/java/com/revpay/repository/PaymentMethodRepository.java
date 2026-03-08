package com.revpay.repository;

import com.revpay.model.entity.PaymentMethod;
import com.revpay.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUser(User user);

    Page<PaymentMethod> findByUser(User user, Pageable pageable);

    List<PaymentMethod> findByUserAndIsDefault(User user, boolean isDefault);

    // Supports direct lookup by ID to avoid loading the full User object in some service contexts
    Page<PaymentMethod> findByUser_UserId(Long userId, Pageable pageable);
}