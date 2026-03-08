package com.revpay.repository;

import com.revpay.model.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUser_UserId(Long userId);

    Page<Loan> findByUser_UserId(Long userId, Pageable pageable);
}