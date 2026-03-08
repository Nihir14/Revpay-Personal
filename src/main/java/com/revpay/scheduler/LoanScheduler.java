package com.revpay.scheduler;

import com.revpay.service.LoanOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanScheduler {

    // INJECT THE NEW SERVICE HERE
    private final LoanOperationService loanOperationService;

    /**
     * Runs daily at 2:00 AM by default.
     * Can be overridden in application.properties using: revpay.scheduler.loan-overdue.cron
     */
    @Scheduled(cron = "${revpay.scheduler.loan-overdue.cron:0 0 2 * * ?}")
    public void runOverdueCheck() {
        log.info("SCHEDULER_START | Initiating daily overdue loan EMI check...");

        try {
            // CALL THE NEW SERVICE HERE
            loanOperationService.markOverdueInstallments();
            log.info("SCHEDULER_SUCCESS | Daily overdue loan EMI check completed successfully.");
        } catch (Exception e) {
            log.error("SCHEDULER_FAILED | Critical error during daily overdue loan check: {}", e.getMessage(), e);
        }
    }
}