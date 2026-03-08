package com.revpay.model.entity;

/**
 * Represents the lifecycle stages of a loan installment.
 */
public enum InstallmentStatus {

    /** Payment is scheduled but not yet due or processed. */
    PENDING,

    /** Payment has been successfully completed. */
    PAID,

    /** Payment was not received by the due date. */
    OVERDUE,

    /** A payment attempt was made but could not be completed (e.g., insufficient funds). */
    FAILED,

    /** The payment for this installment was cancelled by the administrator. */
    CANCELLED
}