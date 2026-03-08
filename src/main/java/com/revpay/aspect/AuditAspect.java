package com.revpay.aspect;

import com.revpay.model.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    @AfterReturning(
            // Reverted processTransfer back to sendMoney here
            pointcut = "execution(* com.revpay.service.WalletService.sendMoney(..)) || " +
                    "execution(* com.revpay.service.WalletService.addFunds(..)) || " +
                    "execution(* com.revpay.service.WalletService.withdrawFunds(..)) || " +
                    "execution(* com.revpay.service.WalletService.payInvoice(..)) || " +
                    "execution(* com.revpay.service.WalletService.acceptRequest(..)) || " +
                    "execution(* com.revpay.service.LoanService.approveLoan(..))",
            returning = "result"
    )
    public void logAuditActivity(JoinPoint joinPoint, Object result) {
        String txnRef = "UNKNOWN";

        if (result instanceof Transaction transaction) {
            txnRef = transaction.getTransactionRef();
        }

        log.info("SECURE AUDIT | Class: {} | Action: {} | TxnRef: {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                txnRef);
    }
}