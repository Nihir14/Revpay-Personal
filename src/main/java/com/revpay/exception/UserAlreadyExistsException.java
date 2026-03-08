package com.revpay.exception;

/**
 * Custom exception to handle cases where a user attempts to register
 * with an email or phone number that already exists in the system.
 */
public class UserAlreadyExistsException extends RuntimeException {

    // Standard constructor that passes the error message to the parent RuntimeException
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}