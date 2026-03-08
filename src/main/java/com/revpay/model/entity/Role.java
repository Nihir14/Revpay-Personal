package com.revpay.model.entity;

/**
 * Defines the user roles within the RevPay ecosystem.
 * These are used by Spring Security for role-based access control (RBAC).
 */
public enum Role {

    /** * Standard retail user.
     * Permissions: Personal transfers, basic wallet management, personal loan applications.
     */
    PERSONAL,

    /** * Commercial or merchant user.
     * Permissions: All PERSONAL features plus Invoice generation and business-specific credit lines.
     */
    BUSINESS,

    /** * Internal platform manager.
     * Permissions: Loan approval, user management, analytics access, and system configuration.
     */
    ADMIN,

    /** * Customer support personnel.
     * Permissions: Read-only access to transactions and user profiles to assist with issues.
     */
    SUPPORT
}