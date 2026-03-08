package com.revpay.model.entity;

/**
 * Represents the various stages of a loan's lifecycle within the RevPay system.
 */
public enum LoanStatus {

    /** Initial state when a business user submits a loan application. */
    APPLIED,

    /** The application is being evaluated by an administrator or credit engine. */
    UNDER_REVIEW,

    /** The loan has been approved but funds have not yet been disbursed to the wallet. */
    APPROVED,

    /** The loan application was denied based on eligibility or risk factors. */
    REJECTED,

    /** Funds have been disbursed, and the loan is currently in the repayment phase. */
    ACTIVE,

    /** The loan has been fully repaid, including all interest and penalties. */
    CLOSED,

    /** The loan was terminated early by the user through a full remaining balance payment. */
    PRE_CLOSED,

    /** The user has failed to pay installments for a prolonged period; legal/recovery action required. */
    DEFAULTED
}