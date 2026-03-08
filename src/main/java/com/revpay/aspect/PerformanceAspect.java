package com.revpay.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    // Threshold in milliseconds to flag abnormally slow service executions
    private static final long SLOW_EXECUTION_THRESHOLD_MS = 1000; // 1 second

    @Around("execution(* com.revpay.service..*(..))")
    public Object profileServiceMethods(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        // Execute the actual service method
        Object output = pjp.proceed();

        long elapsedTime = System.currentTimeMillis() - start;

        String className = pjp.getSignature().getDeclaringType().getSimpleName();
        String methodName = pjp.getSignature().getName();

        // 1. Alert on performance bottlenecks
        if (elapsedTime > SLOW_EXECUTION_THRESHOLD_MS) {
            log.warn("PERFORMANCE ALERT | {}.{}() took {} ms! This exceeds the {} ms threshold.",
                    className, methodName, elapsedTime, SLOW_EXECUTION_THRESHOLD_MS);
        }
        // 2. Keep standard profiling quiet unless actively debugging
        else if (log.isDebugEnabled()) {
            log.debug("PROFILING | {}.{}() - {} ms", className, methodName, elapsedTime);
        }

        return output;
    }
}