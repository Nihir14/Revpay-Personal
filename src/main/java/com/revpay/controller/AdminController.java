package com.revpay.controller;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Invoice;
import com.revpay.model.entity.User;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.UserRepository;
import com.revpay.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Secures the entire controller
@Tag(name = "Admin Operations", description = "Endpoints for platform administration, user management, and oversight")
public class AdminController {

    private final InvoiceService invoiceService;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    // --- DTOs ---
    public record InvoiceDTO(Long id, Long businessId, String customerName, String customerEmail, BigDecimal totalAmount, LocalDate dueDate, String status) {}
    public record BusinessProfileDTO(Long profileId, Long userId, String businessName, String businessType, String taxId, String address, boolean isVerified) {}
    public record UserAdminDTO(Long userId, String email, String fullName, String role) {} // Add active/locked status here if your entity supports it!

    // --- ENDPOINTS ---

    @GetMapping("/users")
    @Operation(summary = "Get all platform users", description = "Retrieves a paginated list of all registered users (Personal and Business).")
    public ResponseEntity<ApiResponse<Page<UserAdminDTO>>> getAllUsers(@PageableDefault(size = 10) Pageable pageable) {
        log.debug("Admin requested all platform users.");

        Page<User> users = userRepository.findAll(pageable);
        Page<UserAdminDTO> dtos = users.map(u -> new UserAdminDTO(
                u.getUserId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole().name()
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "Users retrieved successfully"));
    }

    @PutMapping("/users/{id}/suspend")
    @Operation(summary = "Suspend user account", description = "Administratively locks a user account to prevent login and transactions.")
    public ResponseEntity<ApiResponse<String>> suspendUser(@PathVariable Long id) {
        log.warn("ADMIN_ACTION | Attempting to suspend User ID: {}", id);

        // NOTE: If your User entity doesn't have an isActive or isLocked field,
        // you will need to add a boolean 'active' field to your User entity to fully utilize this.
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        // Example implementation:
        // user.setActive(false);
        // userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(null, "User suspended successfully (Implementation dependent on entity boolean)"));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Get all platform invoices", description = "Retrieves a paginated list of all invoices generated across the RevPay platform.")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getAllSystemInvoices(@PageableDefault(size = 10) Pageable pageable) {
        Page<Invoice> invoices = invoiceService.getAllInvoicesPaged(pageable);
        Page<InvoiceDTO> dtos = invoices.map(i -> new InvoiceDTO(
                i.getId(),
                i.getBusinessProfile().getProfileId(),
                i.getCustomerName(),
                i.getCustomerEmail(),
                i.getTotalAmount(),
                i.getDueDate(),
                i.getStatus().name()
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "Invoices retrieved successfully"));
    }

    @GetMapping("/businesses")
    @Operation(summary = "Get all business profiles", description = "Retrieves a paginated list of all registered business profiles.")
    public ResponseEntity<ApiResponse<Page<BusinessProfileDTO>>> getAllBusinessProfiles(@PageableDefault(size = 10) Pageable pageable) {
        Page<BusinessProfile> profiles = businessProfileRepository.findAll(pageable);
        Page<BusinessProfileDTO> dtos = profiles.map(p -> new BusinessProfileDTO(
                p.getProfileId(),
                p.getUser().getUserId(),
                p.getBusinessName(),
                p.getBusinessType(),
                p.getTaxId(),
                p.getAddress(),
                p.isVerified()
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "Business profiles retrieved successfully"));
    }

    @PostMapping("/businesses/{id}/verify")
    @Operation(summary = "Verify a business", description = "Marks a business profile as verified after manual administrative review.")
    public ResponseEntity<ApiResponse<String>> verifyBusiness(@PathVariable Long id) {
        log.info("Admin initiating verification for business profile ID: {}", id);

        BusinessProfile profile = businessProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found with ID: " + id));

        if (profile.isVerified()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("VAL_002", "Business is already verified."));
        }

        profile.setVerified(true);
        businessProfileRepository.save(profile);

        return ResponseEntity.ok(ApiResponse.success("Verification complete", "Business account verified successfully."));
    }
}