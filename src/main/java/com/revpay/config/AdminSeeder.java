package com.revpay.config;

import com.revpay.model.entity.Role;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (!userRepository.existsByEmail("admin@revpay.com")) {
            log.info("Seeding default Admin user into the database...");

            User admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail("admin@revpay.com");
            admin.setPhoneNumber("0000000000");
            admin.setRole(Role.ADMIN);
            

            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setTransactionPinHash(passwordEncoder.encode("0000"));
            admin.setSecurityQuestion("Admin Code?");
            admin.setSecurityAnswerHash(passwordEncoder.encode("Alpha"));

            userRepository.save(admin);
            log.info("Default Admin User seeded successfully.");
        } else {
            log.info("Admin user already exists. Skipping seed.");
        }
    }
}