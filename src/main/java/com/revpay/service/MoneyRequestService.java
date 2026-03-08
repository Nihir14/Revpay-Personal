package com.revpay.service;

import com.revpay.exception.InvalidStatusException;
import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.model.dto.TransactionRequest;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.User;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoneyRequestService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WalletService walletService; // Injected for actual money movement!

    private String generateRef() {
        return "REQ-" + System.currentTimeMillis();
    }

    @Transactional
    public Transaction requestMoney(Long requesterId, String targetEmail, BigDecimal amount) {
        User requester = userRepository.findById(requesterId).orElseThrow();
        User target = userRepository.findByEmail(targetEmail).orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        Transaction request = new Transaction();
        request.setSender(target); // The target is the one who will SEND the money
        request.setReceiver(requester); // The requester will RECEIVE the money
        request.setAmount(amount);
        request.setType(Transaction.TransactionType.REQUEST);
        request.setStatus(Transaction.TransactionStatus.PENDING);
        request.setTransactionRef(generateRef());
        request.setDescription("Money request from " + requester.getFullName());

        notificationService.createNotification(target.getUserId(), NotificationUtil.requestCreated(amount), "REQUEST");
        log.info("Money requested by {} from {}", requester.getEmail(), targetEmail);
        return transactionRepository.save(request);
    }

    @Transactional
    public Transaction acceptRequest(Long transactionId, String pin) {
        Transaction request = transactionRepository.findById(transactionId).orElseThrow();

        if (request.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new InvalidStatusException("This request cannot be accepted because it is " + request.getStatus());
        }

        // Trigger actual money movement securely!
        TransactionRequest paymentReq = TransactionRequest.builder()
                .receiverIdentifier(request.getReceiver().getEmail())
                .amount(request.getAmount())
                .description("Fulfillment of Request #" + transactionId)
                .transactionPin(pin)
                .idempotencyKey("ACCPT-" + transactionId)
                .build();

        walletService.sendMoney(request.getSender().getUserId(), paymentReq);

        request.setStatus(Transaction.TransactionStatus.COMPLETED);
        notificationService.createNotification(request.getReceiver().getUserId(), NotificationUtil.requestAccepted(request.getAmount()), "REQUEST");
        
        log.info("Request {} fulfilled", transactionId);
        return transactionRepository.save(request);
    }

    @Transactional
    public Transaction declineRequest(Long userId, Long transactionId) {
        Transaction request = transactionRepository.findById(transactionId).orElseThrow();
        if (!request.getReceiver().getUserId().equals(userId)) throw new UnauthorizedException("Not authorized");
        
        request.setStatus(Transaction.TransactionStatus.DECLINED);
        notificationService.createNotification(request.getSender().getUserId(), NotificationUtil.requestDeclined(request.getAmount()), "REQUEST");
        return transactionRepository.save(request);
    }

    @Transactional
    public Transaction cancelOutgoingRequest(Long userId, Long transactionId) {
        Transaction request = transactionRepository.findById(transactionId).orElseThrow();
        if (!request.getSender().getUserId().equals(userId)) throw new UnauthorizedException("Not authorized");
        
        request.setStatus(Transaction.TransactionStatus.CANCELLED);
        return transactionRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getIncomingRequestsPaged(Long userId, Pageable pageable) {
        return transactionRepository.findByReceiverUserIdAndTypeAndStatus(userId, Transaction.TransactionType.REQUEST, Transaction.TransactionStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getOutgoingRequestsPaged(Long userId, Pageable pageable) {
        return transactionRepository.findBySenderUserIdAndTypeAndStatus(userId, Transaction.TransactionType.REQUEST, Transaction.TransactionStatus.PENDING, pageable);
    }
}