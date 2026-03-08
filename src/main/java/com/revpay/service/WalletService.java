package com.revpay.service;

import com.revpay.exception.InsufficientBalanceException;
import com.revpay.exception.InvalidStatusException;
import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.model.entity.*;
import com.revpay.repository.*;
import com.revpay.util.NotificationUtil;
import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.dto.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000.00");
    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("1000.00");

    // --- 1. HELPERS & SECURITY ---

    private String generateRef() { return "TXN-" + System.currentTimeMillis(); }

    private void checkDailyLimit(User sender, BigDecimal newAmount) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        BigDecimal totalSentToday = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(sender, sender).stream()
                .filter(t -> t.getSender() != null && t.getSender().getUserId().equals(sender.getUserId()))
                .filter(t -> t.getType() == Transaction.TransactionType.SEND || t.getType() == Transaction.TransactionType.INVOICE_PAYMENT)
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .filter(t -> t.getTimestamp().isAfter(startOfDay) && t.getTimestamp().isBefore(endOfDay))
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSentToday.add(newAmount).compareTo(DAILY_LIMIT) > 0) {
            log.warn("SECURITY | LIMIT_EXCEEDED | User: {} | Used: {}", sender.getEmail(), totalSentToday);
            throw new IllegalStateException("Daily transfer limit of ₹50,000 exceeded!");
        }
    }

    private void checkAndAlertLowBalance(Wallet wallet) {
        if (wallet.getBalance().compareTo(LOW_BALANCE_THRESHOLD) < 0) {
            notificationService.createNotification(wallet.getUser().getUserId(), NotificationUtil.lowBalanceAlert(wallet.getBalance()), "WALLET");
        }
    }

    // --- 2. CORE WALLET OPERATIONS ---

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return walletRepository.findByUser(user).map(Wallet::getBalance).orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, DeadlockLoserDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2.0))
    @Transactional
    public Transaction sendMoney(Long senderId, TransactionRequest request) {
        // ADDED CUSTOM EXCEPTIONS HERE
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found. Are you using an old JWT token?"));
        User receiver = userRepository.findByEmail(request.getReceiverIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver email not found"));

        if (senderId.equals(receiver.getUserId())) throw new IllegalStateException("Cannot send money to yourself.");
        if (!passwordEncoder.matches(request.getTransactionPin(), sender.getTransactionPinHash())) throw new UnauthorizedException("Invalid PIN!");

        checkDailyLimit(sender, request.getAmount());

        Wallet senderWallet, receiverWallet;
        if (sender.getUserId() < receiver.getUserId()) {
            senderWallet = walletRepository.findByUserUserIdForUpdate(sender.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Sender wallet missing"));
            receiverWallet = walletRepository.findByUserUserIdForUpdate(receiver.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Receiver wallet missing"));
        } else {
            receiverWallet = walletRepository.findByUserUserIdForUpdate(receiver.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Receiver wallet missing"));
            senderWallet = walletRepository.findByUserUserIdForUpdate(sender.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Sender wallet missing"));
        }

        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) throw new InsufficientBalanceException("Insufficient balance!");

        senderWallet.setBalance(senderWallet.getBalance().subtract(request.getAmount()));
        receiverWallet.setBalance(receiverWallet.getBalance().add(request.getAmount()));
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        checkAndAlertLowBalance(senderWallet);

        Transaction tx = new Transaction();
        tx.setSender(sender); tx.setReceiver(receiver); tx.setAmount(request.getAmount());
        tx.setType(Transaction.TransactionType.SEND); tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setTransactionRef(generateRef()); tx.setDescription(request.getDescription() != null ? request.getDescription() : "Transfer");
        tx.setTimestamp(LocalDateTime.now());
        Transaction savedTx = transactionRepository.save(tx);

        eventPublisher.publishEvent(TransferCompletedEvent.builder().transactionId(savedTx.getTransactionId()).senderId(sender.getUserId()).receiverId(receiver.getUserId()).amount(savedTx.getAmount()).senderName(sender.getFullName()).receiverName(receiver.getFullName()).currency("INR").timestamp(savedTx.getTimestamp()).build());
        return savedTx;
    }

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2.0))
    @Transactional
    public Transaction payInvoice(Long userId, Long invoiceId, String pin) {
        // ADDED CUSTOM EXCEPTIONS HERE
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(pin, user.getTransactionPinHash())) throw new UnauthorizedException("Invalid PIN!");

        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) throw new InvalidStatusException("Already paid.");

        Wallet wallet = walletRepository.findByUserUserIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("User wallet not found"));
        if (wallet.getBalance().compareTo(invoice.getTotalAmount()) < 0) throw new InsufficientBalanceException("Insufficient balance!");

        Wallet businessWallet = walletRepository.findByUserUserIdForUpdate(invoice.getBusinessProfile().getUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Business wallet not found"));

        wallet.setBalance(wallet.getBalance().subtract(invoice.getTotalAmount()));
        businessWallet.setBalance(businessWallet.getBalance().add(invoice.getTotalAmount()));

        walletRepository.save(wallet); walletRepository.save(businessWallet);
        checkAndAlertLowBalance(wallet);

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Transaction tx = new Transaction();
        tx.setSender(user); tx.setReceiver(invoice.getBusinessProfile().getUser());
        tx.setAmount(invoice.getTotalAmount()); tx.setType(Transaction.TransactionType.INVOICE_PAYMENT);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED); tx.setTransactionRef(generateRef());
        tx.setDescription("Payment for Invoice #" + invoiceId);

        notificationService.createNotification(userId, NotificationUtil.invoicePaid(invoice.getTotalAmount()), "INVOICE");
        return transactionRepository.save(tx);
    }

    // --- 3. DRY INTERNAL HELPERS FOR FUNDING / WITHDRAWALS ---

    protected Transaction processCredit(Long userId, BigDecimal amount, String desc, Transaction.TransactionType type, String alertMsg) {
        // ADDED CUSTOM EXCEPTIONS HERE
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID " + userId + ". Are you using an old JWT token?"));
        Wallet wallet = walletRepository.findByUserUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for User ID " + userId + "."));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction tx = new Transaction();
        tx.setReceiver(user); tx.setAmount(amount); tx.setType(type);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED); tx.setTransactionRef(generateRef()); tx.setDescription(desc);

        if (alertMsg != null) notificationService.createNotification(userId, alertMsg, "WALLET");
        return transactionRepository.save(tx);
    }

    protected Transaction processDebit(Long userId, BigDecimal amount, String desc, Transaction.TransactionType type, String alertMsg) {
        // ADDED CUSTOM EXCEPTIONS HERE
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID " + userId + ". Are you using an old JWT token?"));
        Wallet wallet = walletRepository.findByUserUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for User ID " + userId + "."));

        if (wallet.getBalance().compareTo(amount) < 0) throw new InsufficientBalanceException("Insufficient balance!");

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        checkAndAlertLowBalance(wallet);

        Transaction tx = new Transaction();
        tx.setSender(user); tx.setAmount(amount); tx.setType(type);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED); tx.setTransactionRef(generateRef()); tx.setDescription(desc);

        if (alertMsg != null) notificationService.createNotification(userId, alertMsg, "WALLET");
        return transactionRepository.save(tx);
    }

    // --- 4. CLEAN ONE-LINE WRAPPERS ---
    // (Notice that @Transactional and @Retryable are ADDED here!)

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, DeadlockLoserDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2.0))
    @Transactional
    public Transaction addFunds(Long userId, BigDecimal amt, String desc) {
        return processCredit(userId, amt, "Added via: " + desc, Transaction.TransactionType.ADD_FUNDS, NotificationUtil.walletCredited(amt));
    }

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, DeadlockLoserDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2.0))
    @Transactional
    public Transaction addFundsForLoan(Long userId, BigDecimal amt, String desc) {
        return processCredit(userId, amt, desc, Transaction.TransactionType.LOAN_DISBURSEMENT, null);
    }

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, DeadlockLoserDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2.0))
    @Transactional
    public Transaction withdrawFunds(Long userId, BigDecimal amt) {
        return processDebit(userId, amt, "Withdrawal to bank account", Transaction.TransactionType.WITHDRAW, NotificationUtil.walletDebited(amt));
    }

    @Retryable(retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class, DeadlockLoserDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2.0))
    @Transactional
    public Transaction withdrawFundsForLoan(Long userId, BigDecimal amt, String desc) {
        return processDebit(userId, amt, desc, Transaction.TransactionType.LOAN_REPAYMENT, null);
    }
}