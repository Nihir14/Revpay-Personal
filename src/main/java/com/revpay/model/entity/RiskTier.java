package com.revpay.model.entity;

/**
 * Defines the risk level of a user based on their credit score and repayment history.
 * Influences interest rates, loan limits, and approval probability.
 */
public enum RiskTier {

    /** High credit score (e.g., 750+); eligible for maximum limits and lowest interest rates. */
    LOW,

    /** Average credit score (e.g., 650-749); eligible for standard limits and rates. */
    MEDIUM,

    /** Low credit score; eligible for restricted limits and higher interest rates. */
    HIGH,

    /** Extreme risk factors; ineligible for any further credit disbursement. */
    VERY_HIGH,

    /** Manually flagged for internal review or suspected fraudulent activity. */
    RESTRICTED
}