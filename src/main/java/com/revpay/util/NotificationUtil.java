package com.revpay.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Prevents accidental instantiation of this utility class
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationUtil {

    /**
     * Helper to ensure amounts always look like "500.00" instead of "500.0000"
     */
    private static String formatAmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public static String moneySent(BigDecimal amount) {
        return String.format("You have sent ₹%s successfully", formatAmt(amount));
    }

    public static String moneyReceived(BigDecimal amount) {
        return String.format("You have received ₹%s", formatAmt(amount));
    }

    public static String requestCreated(BigDecimal amount) {
        return String.format("Money request of ₹%s sent successfully", formatAmt(amount));
    }

    public static String requestAccepted(BigDecimal amount) {
        return String.format("Your money request of ₹%s has been accepted", formatAmt(amount));
    }

    public static String requestDeclined(BigDecimal amount) {
        return String.format("Your money request of ₹%s was declined", formatAmt(amount));
    }

    public static String walletCredited(BigDecimal amount) {
        return String.format("₹%s has been added to your wallet", formatAmt(amount));
    }

    public static String walletDebited(BigDecimal amount) {
        return String.format("₹%s has been withdrawn from your wallet", formatAmt(amount));
    }

    public static String lowBalanceAlert(BigDecimal balance) {
        return String.format("Low balance alert! Your current balance is ₹%s", formatAmt(balance));
    }

    public static String cardAdded() {
        return "A new card has been added to your account";
    }

    public static String cardDeleted() {
        return "A card has been removed from your account";
    }

    public static String cardUpdated() {
        return "Your card details have been updated successfully";
    }

    public static String invoicePaid(BigDecimal amount) {
        return String.format("Invoice of ₹%s paid successfully", formatAmt(amount));
    }

    public static String loanApplied(BigDecimal amount) {
        return String.format("Loan application of ₹%s submitted successfully", formatAmt(amount));
    }

    public static String loanApproved() {
        return "Your loan has been approved! Amount will be credited to your wallet shortly.";
    }

    public static String loanRejected() {
        return "Your loan application has been rejected.";
    }

    public static String loanRepayment(BigDecimal amount) {
        return String.format("EMI payment of ₹%s received successfully", formatAmt(amount));
    }
}