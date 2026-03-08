package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.dto.WalletAnalyticsDTO;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.User;
import com.revpay.model.entity.Wallet;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAnalyticsService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository; // <-- NEW: Injected to look up the user

    @Transactional(readOnly = true)
    public WalletAnalyticsDTO getSpendingAnalytics(Long userId) { // <-- NEW: Accepts Long instead of User
        log.info("Generating spending analytics for userId: {}", userId);

        // <-- NEW: We fetch the user safely inside the service!
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Transaction> history = transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);

        List<Transaction> outgoing = history.stream()
                .filter(t -> t.getSender() != null && t.getSender().getUserId().equals(userId))
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .toList();

        BigDecimal totalSpent = outgoing.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categories = outgoing.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getType().name(),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        BigDecimal currentBalance = walletRepository.findByUser(user)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal averageTransactionValue = outgoing.isEmpty() ? BigDecimal.ZERO :
                totalSpent.divide(BigDecimal.valueOf(outgoing.size()), 2, RoundingMode.HALF_UP);

        return WalletAnalyticsDTO.builder()
                .currentBalance(currentBalance)
                .totalSpent(totalSpent)
                .spendingByCategory(categories)
                .transactionCount((long) outgoing.size())
                .averageTransactionValue(averageTransactionValue)
                .monthlyChangePercentage(0.0)
                .currency("INR")
                .build();
    }
}