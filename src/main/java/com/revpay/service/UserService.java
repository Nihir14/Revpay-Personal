package com.revpay.service;

import com.revpay.controller.UserController.UserProfileDTO;
import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.model.dto.UpdatePasswordRequest;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserProfileDTO(user.getUserId(), user.getEmail(), user.getFullName(), user.getPhoneNumber(), user.getRole().name());
    }

    @Transactional
    public void updateProfile(Long userId, String fullName, String phoneNumber) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        userRepository.save(user);
        log.info("Profile updated for UserID: {}", userId);
    }

    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("The current password provided is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void updatePin(Long userId, String oldPin, String newPin) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(oldPin, user.getTransactionPinHash())) {
            throw new UnauthorizedException("The current PIN provided is incorrect.");
        }
        user.setTransactionPinHash(passwordEncoder.encode(newPin));
        userRepository.save(user);
        log.info("PIN updated for UserID: {}", userId);
    }
}