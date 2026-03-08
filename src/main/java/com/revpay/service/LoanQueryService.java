package com.revpay.service;

import com.revpay.model.entity.InstallmentStatus;
import com.revpay.model.entity.Loan;
import com.revpay.model.entity.LoanInstallment;
import com.revpay.model.entity.LoanStatus;
import com.revpay.repository.LoanInstallmentRepository;
import com.revpay.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanQueryService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;

    @Transactional(readOnly = true)
    public List<Loan> getUserLoans(Long userId) {
        return loanRepository.findByUser_UserId(userId);
    }

    @Transactional(readOnly = true)
    public Page<Loan> getUserLoansPaged(Long userId, Pageable pageable) {
        return loanRepository.findByUser_UserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Loan> getAllLoansPaged(Pageable pageable) {
        return loanRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalOutstanding(Long userId) {
        return loanRepository.findByUser_UserId(userId).stream()
                .filter(l -> l.getStatus() == LoanStatus.ACTIVE)
                .map(Loan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPaid(Long userId) {
        return installmentRepository.findByUserIdAndStatus(userId, InstallmentStatus.PAID).stream()
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPending(Long userId) {
        return installmentRepository.findByUserIdAndStatus(userId, InstallmentStatus.PENDING).stream()
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<LoanInstallment> getEmiSchedule(Long userId, Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (!loan.getUser().getUserId().equals(userId)) throw new IllegalArgumentException("Unauthorized");
        return installmentRepository.findByLoan_LoanId(loanId);
    }

    @Transactional(readOnly = true)
    public Page<LoanInstallment> getEmiSchedulePaged(Long userId, Long loanId, Pageable pageable) {
        Loan loan = loanRepository.findById(loanId).orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        if (!loan.getUser().getUserId().equals(userId)) throw new IllegalArgumentException("Unauthorized");
        return installmentRepository.findByLoan_LoanId(loanId, pageable);
    }

    @Transactional(readOnly = true)
    public List<LoanInstallment> getOverdueEmis(Long userId) {
        List<LoanInstallment> overdueList = new ArrayList<>(installmentRepository.findByUserIdAndStatus(userId, InstallmentStatus.OVERDUE));
        installmentRepository.findByUserIdAndStatus(userId, InstallmentStatus.PENDING).stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()))
                .forEach(overdueList::add);
        return overdueList;
    }

    @Transactional(readOnly = true)
    public Page<LoanInstallment> getOverdueEmisPaged(Long userId, Pageable pageable) {
        return installmentRepository.findOverdueByUserId(userId, LocalDate.now(), pageable);
    }
}