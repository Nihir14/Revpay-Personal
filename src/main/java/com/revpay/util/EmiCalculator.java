package com.revpay.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

// Prevents accidental instantiation of this utility class
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmiCalculator {

    public static BigDecimal calculateEMI(
            BigDecimal principal,
            BigDecimal annualRate,
            int months
    ) {
        // 1. Safety checks for invalid inputs
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (months <= 0) {
            throw new IllegalArgumentException("Tenure in months must be greater than zero.");
        }

        // 2. Handle 0% interest edge case to prevent 'Division by Zero' ArithmeticException
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        // 3. Standard EMI Calculation
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(12 * 100), MathContext.DECIMAL64);

        BigDecimal onePlusRPowerN =
                (BigDecimal.ONE.add(monthlyRate)).pow(months, MathContext.DECIMAL64);

        BigDecimal rawEmi = principal
                .multiply(monthlyRate)
                .multiply(onePlusRPowerN)
                .divide(onePlusRPowerN.subtract(BigDecimal.ONE), MathContext.DECIMAL64);

        // 4. Round the final result to 2 decimal places for standard currency representation
        return rawEmi.setScale(2, RoundingMode.HALF_UP);
    }
}