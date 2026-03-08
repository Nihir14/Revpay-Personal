package com.revpay.service;

import com.revpay.model.dto.*;
import com.revpay.model.entity.*;
import com.revpay.repository.LoanInstallmentRepository;
import com.revpay.repository.LoanRepository;
import com.revpay.repository.UserRepository;
import com.revpay.util.EmiCalculator;
import com.revpay.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanOperationService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final LoanInstallmentRepository installmentRepository;
    private final NotificationService notificationService;
    private final CreditScoringService creditScoringService;

    private static final BigDecimal PRE_CLOSURE_RATE   = new BigDecimal("0.02");
    private static final BigDecimal OVERDUE_PENALTY    = new BigDecimal("100");

    @Transactional
    public LoanResponseDTO applyLoan(Long userId, LoanApplyDTO dto) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getRole() != Role.BUSINESS) throw new IllegalArgumentException("Only business users can apply");
        if (!creditScoringService.isEligibleForLoan(userId, dto.getAmount())) throw new IllegalArgumentException("Amount exceeds eligibility limit");

        Loan loan = Loan.builder()
                .user(user).amount(dto.getAmount()).tenureMonths(dto.getTenureMonths())
                .remainingAmount(dto.getAmount()).purpose(dto.getPurpose()).status(LoanStatus.APPLIED).build();
        loanRepository.save(loan);
        notificationService.createNotification(userId, NotificationUtil.loanApplied(dto.getAmount()), "LOAN");

        return LoanResponseDTO.builder()
                .loanId(loan.getLoanId()).amount(loan.getAmount()).emiAmount(BigDecimal.ZERO)
                .remainingAmount(loan.getRemainingAmount()).status(loan.getStatus()).build();
    }

    @Transactional
    public String approveLoan(LoanApprovalDTO dto) {
        Loan loan = loanRepository.findById(dto.getLoanId()).orElseThrow();
        if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.CLOSED) throw new IllegalArgumentException("Invalid state");

        if (dto.getApproved()) {
            BigDecimal interest = dto.getInterestRate() != null ? dto.getInterestRate() : creditScoringService.getDynamicInterest(loan.getUser().getUserId());
            loan.setInterestRate(interest);
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setStartDate(LocalDate.now());
            loan.setEndDate(LocalDate.now().plusMonths(loan.getTenureMonths()));
            loan.setEmiAmount(EmiCalculator.calculateEMI(loan.getAmount(), interest, loan.getTenureMonths()));

            walletService.addFundsForLoan(loan.getUser().getUserId(), loan.getAmount(), "Loan Disbursement #" + loan.getLoanId());
            generateInstallments(loan);
            notificationService.createNotification(loan.getUser().getUserId(), NotificationUtil.loanApproved(), "LOAN");
        } else {
            loan.setStatus(LoanStatus.REJECTED);
            notificationService.createNotification(loan.getUser().getUserId(), NotificationUtil.loanRejected(), "LOAN");
        }
        loanRepository.save(loan);
        return "Loan decision processed";
    }

    private void generateInstallments(Loan loan) {
        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan).installmentNumber(i).amount(loan.getEmiAmount())
                    .dueDate(LocalDate.now().plusMonths(i)).status(InstallmentStatus.PENDING).build();
            installmentRepository.save(installment);
        }
    }

    @Transactional
    public String repayLoan(Long userId, LoanRepayDTO dto) {
        Loan loan = loanRepository.findById(dto.getLoanId()).orElseThrow();
        if (!loan.getUser().getUserId().equals(userId)) throw new IllegalArgumentException("Unauthorized");
        if (loan.getStatus() == LoanStatus.CLOSED) throw new IllegalArgumentException("Loan is closed");

        LoanInstallment nextInstallment = installmentRepository.findByLoan_LoanId(loan.getLoanId()).stream()
                .filter(i -> i.getStatus() == InstallmentStatus.PENDING || i.getStatus() == InstallmentStatus.OVERDUE)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No pending EMI"));

        BigDecimal payableAmount = nextInstallment.getAmount();
        if (nextInstallment.getStatus() == InstallmentStatus.OVERDUE) payableAmount = payableAmount.add(OVERDUE_PENALTY);

        walletService.withdrawFundsForLoan(userId, payableAmount, "Loan Repayment #" + loan.getLoanId());
        
        nextInstallment.setStatus(InstallmentStatus.PAID);
        installmentRepository.save(nextInstallment);
        
        loan.setRemainingAmount(loan.getRemainingAmount().subtract(payableAmount));
        if (loan.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setRemainingAmount(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanRepository.save(loan);
        notificationService.createNotification(userId, NotificationUtil.loanRepayment(payableAmount), "LOAN");
        return "EMI Paid Successfully";
    }

    @Transactional
    public String preCloseLoan(Long userId, Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElseThrow();
        if (!loan.getUser().getUserId().equals(userId)) throw new IllegalArgumentException("Unauthorized");
        if (loan.getStatus() == LoanStatus.CLOSED) throw new IllegalArgumentException("Loan is already closed");

        BigDecimal totalPayable = loan.getRemainingAmount().add(loan.getRemainingAmount().multiply(PRE_CLOSURE_RATE));
        walletService.withdrawFundsForLoan(userId, totalPayable, "Loan Pre-Closure #" + loanId);

        loan.setRemainingAmount(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.CLOSED);
        loanRepository.save(loan);
        notificationService.createNotification(userId, "Your loan has been pre-closed successfully.", "LOAN");
        return "Loan Pre-closed successfully";
    }

    // Restored your Scheduled Job method!
    @Transactional
    public void markOverdueInstallments() {
        List<LoanInstallment> pendingInstallments = installmentRepository.findAllByStatus(InstallmentStatus.PENDING);
        List<LoanInstallment> nowOverdue = pendingInstallments.stream()
                .filter(i -> i.getDueDate().isBefore(LocalDate.now()))
                .toList();

        nowOverdue.forEach(emi -> {
            emi.setStatus(InstallmentStatus.OVERDUE);
            notificationService.createNotification(emi.getLoan().getUser().getUserId(), "Your EMI is overdue. Please pay immediately.", "LOAN");
        });

        installmentRepository.saveAll(nowOverdue);
        log.info("Overdue EMI check completed. {} installments marked overdue.", nowOverdue.size());
    }
}