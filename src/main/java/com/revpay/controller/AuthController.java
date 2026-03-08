package com.revpay.controller;

import com.revpay.model.dto.*;
import com.revpay.security.JwtUtils;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Operations", description = "Endpoints for user registration, login, and password recovery")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns a JWT token.")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Authentication attempt for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateTokenFromUsername(loginRequest.getEmail());

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        JwtResponse jwtResponse = new JwtResponse(jwt, userDetails.getUserId(), userDetails.getEmail(), role);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new RevPay user account.")
    public ResponseEntity<ApiResponse<String>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Get security question", description = "Retrieves the security question for password recovery.")
    public ResponseEntity<ApiResponse<String>> getSecurityQuestion(@Valid @RequestBody ForgotPasswordRequest request) {
        String question = authService.getSecurityQuestion(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(question, "Security question retrieved successfully"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset forgotten password", description = "Resets the password using the security answer.")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password has been reset successfully"));
    }
}