package com.revpay.repository;

import com.revpay.model.entity.InstallmentStatus;
import com.revpay.model.entity.LoanInstallment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, Long> {

    List<LoanInstallment> findByLoan_LoanId(Long loanId);

    Page<LoanInstallment> findByLoan_LoanId(Long loanId, Pageable pageable);

    @Query("SELECT i FROM LoanInstallment i WHERE i.loan.user.userId = :userId AND i.status = :status")
    List<LoanInstallment> findByUserIdAndStatus(@Param("userId") Long userId,
                                                @Param("status") InstallmentStatus status);

    @Query("SELECT i FROM LoanInstallment i WHERE i.loan.user.userId = :userId AND i.status = :status")
    Page<LoanInstallment> findByUserIdAndStatus(@Param("userId") Long userId,
                                                @Param("status") InstallmentStatus status,
                                                Pageable pageable);

    @Query("SELECT i FROM LoanInstallment i WHERE i.status = :status")
    List<LoanInstallment> findAllByStatus(@Param("status") InstallmentStatus status);

    @Query("SELECT i FROM LoanInstallment i WHERE i.loan.user.userId = :userId AND (i.status = 'OVERDUE' OR (i.status = 'PENDING' AND i.dueDate < :currentDate))")
    Page<LoanInstallment> findOverdueByUserId(@Param("userId") Long userId,
                                              @Param("currentDate") LocalDate currentDate,
                                              Pageable pageable);
}