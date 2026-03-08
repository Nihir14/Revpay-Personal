package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.model.entity.PaymentMethod;
import com.revpay.model.entity.User;
import com.revpay.repository.PaymentMethodRepository;
import com.revpay.repository.UserRepository;
import com.revpay.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public PaymentMethod addCard(Long userId, PaymentMethod card) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean exists = paymentMethodRepository.findByUser(user).stream()
                .anyMatch(c -> c.getCardNumber().equals(card.getCardNumber()));
        if (exists) throw new IllegalStateException("This card is already linked to your account.");

        card.setUser(user);

        notificationService.createNotification(userId, NotificationUtil.cardAdded(), "SECURITY");
        log.info("Card added successfully for UserID: {}", userId);
        return paymentMethodRepository.save(card);
    }

    @Transactional
    public PaymentMethod updateCard(Long userId, Long cardId, PaymentMethod updatedCard) {
        PaymentMethod existingCard = paymentMethodRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!existingCard.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to update this card");
        }

        if (updatedCard.getExpiryDate() != null) {
            existingCard.setExpiryDate(updatedCard.getExpiryDate());
        }
        if (updatedCard.getBillingAddress() != null) {
            existingCard.setBillingAddress(updatedCard.getBillingAddress());
        }

        notificationService.createNotification(userId, NotificationUtil.cardUpdated(), "SECURITY");
        return paymentMethodRepository.save(existingCard);
    }

    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        PaymentMethod card = paymentMethodRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!card.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this card.");
        }

        paymentMethodRepository.delete(card);
        notificationService.createNotification(userId, NotificationUtil.cardDeleted(), "SECURITY");
        log.info("Card {} deleted for UserID: {}", cardId, userId);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> getCards(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return paymentMethodRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Page<PaymentMethod> getCardsPaged(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow();
        return paymentMethodRepository.findByUser(user, pageable);
    }

    @Transactional
    public void setDefaultCard(Long userId, Long cardId) {
        User user = userRepository.findById(userId).orElseThrow();

        List<PaymentMethod> oldDefaults = paymentMethodRepository.findByUserAndIsDefault(user, true);
        oldDefaults.forEach(c -> c.setDefault(false));
        paymentMethodRepository.saveAll(oldDefaults);

        PaymentMethod newDefault = paymentMethodRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        if (!newDefault.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to modify this card");
        }

        newDefault.setDefault(true);
        paymentMethodRepository.save(newDefault);
        log.info("Card {} set as default for UserID: {}", cardId, userId);
    }
}