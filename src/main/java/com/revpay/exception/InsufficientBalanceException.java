package com.revpay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a wallet transaction is attempted with funds 
 * lower than the required amount.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}