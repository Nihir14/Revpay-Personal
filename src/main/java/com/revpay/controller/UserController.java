package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.dto.UpdatePasswordRequest;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User Profile Management", description = "Endpoints for managing the logged-in user's profile, password, and PIN")
public class UserController {

    private final UserService userService;

    // Local DTOs for cleaner code
    public record UpdateProfileRequest(@NotBlank String fullName, @NotBlank String phoneNumber) {}
    public record UpdatePinRequest(@NotBlank String oldPin, @NotBlank String newPin) {}
    public record UserProfileDTO(Long userId, String email, String fullName, String phoneNumber, String role) {}

    private Long getAuthId(Authentication auth) {
        return ((UserDetailsImpl) auth.getPrincipal()).getUserId();
    }

    @GetMapping
    @Operation(summary = "Get my profile", description = "Retrieves the profile information of the logged-in user.")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserProfile(getAuthId(auth)), "Profile retrieved"));
    }

    @PutMapping
    @Operation(summary = "Update profile information", description = "Updates the user's name and phone number.")
    public ResponseEntity<ApiResponse<String>> updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication auth) {
        userService.updateProfile(getAuthId(auth), request.fullName(), request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success(null, "Profile updated successfully"));
    }

    @PutMapping("/password")
    @Operation(summary = "Update password", description = "Updates the password using current password verification.")
    public ResponseEntity<ApiResponse<String>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request, Authentication auth) {
        userService.updatePassword(getAuthId(auth), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password updated successfully"));
    }

    @PutMapping("/pin")
    @Operation(summary = "Set/change transaction PIN", description = "Updates the user's secure transaction PIN.")
    public ResponseEntity<ApiResponse<String>> updatePin(@Valid @RequestBody UpdatePinRequest request, Authentication auth) {
        userService.updatePin(getAuthId(auth), request.oldPin(), request.newPin());
        return ResponseEntity.ok(ApiResponse.success(null, "Transaction PIN updated successfully"));
    }
}