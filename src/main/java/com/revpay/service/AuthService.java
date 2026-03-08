package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.exception.UserAlreadyExistsException;
import com.revpay.model.dto.ResetPasswordRequest;
import com.revpay.model.dto.SignupRequest;
import com.revpay.model.dto.UpdatePasswordRequest;
import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Role;
import com.revpay.model.entity.User;
import com.revpay.model.entity.Wallet;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor // Replaces @Autowired field injection
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final BusinessProfileRepository businessRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(SignupRequest request) {
        log.info("REGISTRATION_START | Attempting to register email: {}", request.getEmail());

        // 1. Validation Logic
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("REGISTRATION_FAILED | Email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException("The email address is already registered.");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            log.warn("REGISTRATION_FAILED | Phone number already exists: {}", request.getPhoneNumber());
            throw new UserAlreadyExistsException("The phone number is already in use.");
        }

        // 2. User Entity Preparation
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());

        // Hashing sensitive data
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setTransactionPinHash(passwordEncoder.encode(request.getTransactionPin()));
        user.setSecurityQuestion(request.getSecurityQuestion());
        user.setSecurityAnswerHash(passwordEncoder.encode(request.getSecurityAnswer()));

        // 3. Role Assignment
        assignRole(user, request.getRole());

        // 4. Persistence & Side-Effects
        User savedUser = userRepository.save(user);
        initializeUserWallet(savedUser);

        if (savedUser.getRole() == Role.BUSINESS) {
            initializeBusinessProfile(savedUser, request.getFullName());
        }

        log.info("REGISTRATION_SUCCESS | Created UserID: {} with Role: {}", savedUser.getUserId(), savedUser.getRole());
    }

    public String getSecurityQuestion(String email) {
        log.debug("SECURITY_QUESTION_REQUEST | For email: {}", email);
        return userRepository.findByEmail(email)
                .map(User::getSecurityQuestion)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with the provided email."));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("PASSWORD_RESET_ATTEMPT | Initiating for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Verify security answer
        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswerHash())) {
            log.warn("PASSWORD_RESET_FAILED | Invalid security answer for email: {}", request.getEmail());
            throw new UnauthorizedException("Verification failed: Security answer is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("PASSWORD_RESET_SUCCESS | Password updated for email: {}", request.getEmail());
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        log.info("PASSWORD_UPDATE_ATTEMPT | Processing for UserID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User session is invalid."));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            log.warn("PASSWORD_UPDATE_FAILED | Current password mismatch for UserID: {}", userId);
            throw new UnauthorizedException("The current password provided is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("PASSWORD_UPDATE_SUCCESS | UserID: {} successfully updated their password.", userId);
    }

    // --- Private Helper Methods to keep code DRY and Readable ---

    private void assignRole(User user, String roleStr) {
        if (roleStr == null) {
            user.setRole(Role.PERSONAL);
            return;
        }
        try {
            user.setRole(Role.valueOf(roleStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("INVALID_ROLE_PROVIDED | Received: {}. Defaulting to PERSONAL.", roleStr);
            user.setRole(Role.PERSONAL);
        }
    }

    private void initializeUserWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        walletRepository.save(wallet);
        log.debug("WALLET_INITIALIZED | Linked to UserID: {}", user.getUserId());
    }

    private void initializeBusinessProfile(User user, String name) {
        BusinessProfile profile = new BusinessProfile();
        profile.setUser(user);
        profile.setBusinessName(name + " Business");
        // Business profiles usually start as unverified
        businessRepository.save(profile);
        log.debug("BUSINESS_PROFILE_CREATED | Linked to UserID: {}", user.getUserId());
    }
}