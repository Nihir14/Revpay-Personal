package com.revpay.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Defines the cross-cutting concern: intercepting all methods
     * within the controller and service packages.
     */
    @Pointcut("within(com.revpay.controller..*) || within(com.revpay.service..*)")
    public void applicationPackagePointcut() {
        // Pointcut definition intentionally left blank
    }

    /**
     * Logs method entry.
     * Changed to DEBUG level to prevent log-flooding in production environments.
     */
    @Before("applicationPackagePointcut()")
    public void logBefore(JoinPoint joinPoint) {
        if (log.isDebugEnabled()) {
            log.debug("ENTER | {}.{}()",
                    joinPoint.getSignature().getDeclaringTypeName(),
                    joinPoint.getSignature().getName());
        }
    }

    /**
     * Logs exceptions thrown from intercepted methods.
     * Upgraded to capture the explicit error message alongside the root cause.
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("EXCEPTION | {}.{}() | Message: {} | Cause: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getMessage(),
                e.getCause() != null ? e.getCause() : "N/A");
    }
}