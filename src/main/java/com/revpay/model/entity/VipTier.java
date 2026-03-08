package com.revpay.model.entity;

/**
 * Defines the loyalty tiers for RevPay users.
 * Tiers are typically assigned based on credit score, transaction volume, or account balance.
 */
public enum VipTier {

    /** Default tier for all users. No specific interest rate or fee discounts. */
    NONE,

    /** Intermediate tier. Eligible for moderate interest rate discounts (e.g., -1%). */
    GOLD,

    /** High-value tier. Eligible for significant interest rate discounts (e.g., -2%) and higher limits. */
    PLATINUM,

    /** Premier tier for UHNW and high-volume business users. Eligible for maximum benefits. */
    DIAMOND,

    /** Internal tier for corporate partners or staff accounts with custom benefit profiles. */
    CORPORATE
}