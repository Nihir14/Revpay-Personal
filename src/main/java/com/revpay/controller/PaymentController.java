package com.revpay.controller;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.PaymentMethod;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import com.revpay.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'BUSINESS')") // Ensures only valid account holders can manage cards
@Tag(name = "Payment Methods", description = "Endpoints for managing linked debit/credit cards and billing addresses")
public class PaymentController {

    private final PaymentMethodService paymentMethodService;

    private final UserRepository userRepository;

    // --- DTOs ---
    public record PaymentMethodDTO(Long id, String partialCardNumber, String expiryDate, String billingAddress, boolean isDefault, String cardType) {}

    public record CardAddRequest(
            @NotBlank(message = "Card number is required") String cardNumber,
            @NotBlank(message = "Expiry date is required")
            @Pattern(regexp = "^(0[1-9]|1[0-2])/?([0-9]{2})$", message = "Expiry date must be in MM/YY format") String expiryDate,
            @NotBlank(message = "CVV is required") String cvv,
            String billingAddress,
            String cardType
    ) {}

    // --- HELPER METHODS ---
    private User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database"));
    }

    private String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return cardNumber;
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    // --- ENDPOINTS ---

    @PostMapping // Changed from /add to strictly use the POST method on the resource
    @Operation(summary = "Add a new card", description = "Links a new credit or debit card to the authenticated user's digital wallet.")
    public ResponseEntity<ApiResponse<PaymentMethodDTO>> addCard(
            @Valid @RequestBody CardAddRequest request,
            Authentication auth) {

        User user = getAuthenticatedUser(auth);
        log.info("User ID: {} is linking a new {} card", user.getUserId(), request.cardType());

        PaymentMethod card = new PaymentMethod();
        card.setCardNumber(request.cardNumber());
        card.setExpiryDate(request.expiryDate());
        card.setBillingAddress(request.billingAddress());
        card.setCvv(request.cvv()); // FIX: CVV mapped securely

        if (request.cardType() != null) {
            try {
                card.setCardType(PaymentMethod.CardType.valueOf(request.cardType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid card type provided: {}", request.cardType());
            }
        }

        PaymentMethod saved = paymentMethodService.addCard(user.getUserId(), card);

        PaymentMethodDTO dto = new PaymentMethodDTO(
                saved.getId(),
                maskCard(saved.getCardNumber()),
                saved.getExpiryDate(),
                saved.getBillingAddress(),
                saved.isDefault(),
                saved.getCardType() != null ? saved.getCardType().name() : null
        );

        return ResponseEntity.ok(ApiResponse.success(dto, "Card added successfully"));
    }

    @GetMapping // Changed from /my-cards to strictly use the GET method on the resource
    @Operation(summary = "Get my saved cards", description = "Retrieves a paginated list of all masked payment methods linked to the user.")
    public ResponseEntity<ApiResponse<Page<PaymentMethodDTO>>> getMyCards(
            Authentication auth,
            @PageableDefault(size = 10) Pageable pageable) {

        User user = getAuthenticatedUser(auth);
        Page<PaymentMethod> cards = paymentMethodService.getCardsPaged(user.getUserId(), pageable);

        Page<PaymentMethodDTO> dtos = cards.map(c -> new PaymentMethodDTO(
                c.getId(),
                maskCard(c.getCardNumber()),
                c.getExpiryDate(),
                c.getBillingAddress(),
                c.isDefault(),
                c.getCardType() != null ? c.getCardType().name() : null
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "Cards retrieved successfully"));
    }

    @PatchMapping("/{cardId}/default")
    @Operation(summary = "Set default card", description = "Updates a specific card to be the primary payment method for transactions.")
    public ResponseEntity<ApiResponse<String>> makeDefault(
            @Parameter(description = "ID of the card to set as default") @PathVariable Long cardId,
            Authentication auth) {

        User user = getAuthenticatedUser(auth);
        paymentMethodService.setDefaultCard(user.getUserId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(null, "Default card updated successfully"));
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Delete a saved card", description = "Removes a specific payment method from the user's digital wallet.")
    public ResponseEntity<ApiResponse<String>> deleteCard(
            @Parameter(description = "ID of the card to remove") @PathVariable Long cardId,
            Authentication auth) {

        User user = getAuthenticatedUser(auth);
        paymentMethodService.deleteCard(user.getUserId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(null, "Card removed successfully"));
    }
}