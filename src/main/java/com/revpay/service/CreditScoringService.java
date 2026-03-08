package com.revpay.service;

import com.revpay.model.dto.LoanEligibilityDTO;
import com.revpay.model.dto.LoanRecommendationDTO;
import com.revpay.model.entity.InstallmentStatus;
import com.revpay.model.entity.RiskTier;
import com.revpay.model.entity.VipTier;
import com.revpay.repository.LoanInstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoringService {

    private final LoanInstallmentRepository installmentRepository;
    
    private static final BigDecimal MEDIUM_RISK_FACTOR = new BigDecimal("0.7");
    private static final BigDecimal HIGH_RISK_FACTOR   = new BigDecimal("0.4");
    private static final BigDecimal MIN_INTEREST_RATE  = new BigDecimal("1");

    @Transactional(readOnly = true)
    public int calculateCreditScore(Long userId) {
        long paid = installmentRepository.findByUserIdAndStatus(userId, InstallmentStatus.PAID).size();
        long overdue = installmentRepository.findByUserIdAndStatus(userId, InstallmentStatus.OVERDUE).size();
        int score = 700 + (int)(paid * 2) - (int)(overdue * 5);
        return Math.max(300, Math.min(850, score));
    }

    @Transactional(readOnly = true)
    public RiskTier getRiskTier(Long userId) {
        int score = calculateCreditScore(userId);
        return (score >= 750) ? RiskTier.LOW : (score >= 650) ? RiskTier.MEDIUM : RiskTier.HIGH;
    }

    @Transactional(readOnly = true)
    public BigDecimal getLoanLimit(Long userId) {
        int score = calculateCreditScore(userId);
        return (score >= 750) ? BigDecimal.valueOf(1_000_000)
             : (score >= 700) ? BigDecimal.valueOf(500_000)
             : (score >= 650) ? BigDecimal.valueOf(200_000)
             : BigDecimal.valueOf(50_000);
    }

    @Transactional(readOnly = true)
    public boolean isEligibleForLoan(Long userId, BigDecimal requestedAmount) {
        if (getRiskTier(userId) == RiskTier.HIGH) return false;
        return requestedAmount.compareTo(getLoanLimit(userId)) <= 0;
    }

    @Transactional(readOnly = true)
    public LoanEligibilityDTO checkEligibility(Long userId) {
        int score = calculateCreditScore(userId);
        RiskTier risk = getRiskTier(userId);
        return LoanEligibilityDTO.builder()
                .creditScore(score)
                .riskTier(risk)
                .maxEligibleAmount(getLoanLimit(userId))
                .eligible(risk != RiskTier.HIGH)
                .build();
    }

    @Transactional(readOnly = true)
    public VipTier getVipTier(Long userId) {
        int score = calculateCreditScore(userId);
        return (score >= 780) ? VipTier.PLATINUM : (score >= 720) ? VipTier.GOLD : VipTier.NONE;
    }

    public BigDecimal getDynamicInterest(Long userId) {
        int score = calculateCreditScore(userId);
        BigDecimal interest = (score >= 750) ? BigDecimal.valueOf(8)
                : (score >= 700) ? BigDecimal.valueOf(10)
                : (score >= 650) ? BigDecimal.valueOf(12)
                : BigDecimal.valueOf(15);
        
        VipTier vip = getVipTier(userId);
        BigDecimal finalInterest = switch (vip) {
            case PLATINUM -> interest.subtract(BigDecimal.valueOf(2));
            case GOLD     -> interest.subtract(BigDecimal.valueOf(1));
            default       -> interest;
        };
        return finalInterest.compareTo(MIN_INTEREST_RATE) < 0 ? MIN_INTEREST_RATE : finalInterest;
    }

    @Transactional(readOnly = true)
    public LoanRecommendationDTO getLoanRecommendation(Long userId) {
        RiskTier risk = getRiskTier(userId);
        BigDecimal limit = getLoanLimit(userId);

        BigDecimal recommended = switch (risk) {
            case LOW    -> limit;
            case MEDIUM -> limit.multiply(MEDIUM_RISK_FACTOR);
            default     -> limit.multiply(HIGH_RISK_FACTOR);
        };

        return LoanRecommendationDTO.builder()
                .creditScore(calculateCreditScore(userId))
                .riskTier(risk)
                .vipTier(getVipTier(userId))
                .recommendedAmount(recommended)
                .expectedInterestRate(getDynamicInterest(userId))
                .logicReasoning("Recommendation based on internal credit score.")
                .build();
    }
}