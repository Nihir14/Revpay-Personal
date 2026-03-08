package com.revpay.controller;

import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.Invoice;
import com.revpay.security.UserDetailsImpl;
import com.revpay.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/invoices") // Accessible to standard users
@RequiredArgsConstructor
@Tag(name = "Customer Invoicing", description = "Endpoints for customers to view and pay their invoices")
public class CustomerInvoiceController {

    private final InvoiceService invoiceService;

    // DTO for returning clean JSON
    public record InvoiceDTO(Long id, Long businessId, String customerName, String customerEmail, BigDecimal totalAmount, LocalDate dueDate, String status) {}

    // ---> NEW INBOX ENDPOINT <---
    @GetMapping
    @Operation(summary = "Get my invoices", description = "Retrieves a paginated list of all invoices sent to the logged-in user's email.")
    public ResponseEntity<ApiResponse<Page<InvoiceDTO>>> getMyInvoices(
            Principal principal,
            @PageableDefault(size = 10) Pageable pageable) {

        String loggedInEmail = principal.getName(); // JWT filter sets email as the principal name
        log.info("Customer {} is checking their invoice inbox", loggedInEmail);

        Page<Invoice> invoices = invoiceService.getMyInvoices(loggedInEmail, pageable);

        Page<InvoiceDTO> dtos = invoices.map(i -> new InvoiceDTO(
                i.getId(),
                i.getBusinessProfile().getProfileId(),
                i.getCustomerName(),
                i.getCustomerEmail(),
                i.getTotalAmount(),
                i.getDueDate(),
                i.getStatus().name()
        ));

        return ResponseEntity.ok(ApiResponse.success(dtos, "Your invoices retrieved successfully"));
    }

    // ---> EXISTING PAY ENDPOINT <---
    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay an invoice via wallet", description = "Allows a customer to pay an invoice directly from their RevPay wallet balance.")
    public ResponseEntity<ApiResponse<String>> payInvoiceViaWallet(
            @Parameter(description = "ID of the invoice to pay") @PathVariable Long id,
            @Parameter(description = "The user's transaction PIN") @RequestParam String pin,
            Authentication auth) {

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long customerId = userDetails.getUserId();

        log.info("Customer ID: {} is attempting to pay invoice ID: {}", customerId, id);

        invoiceService.payInvoiceViaWallet(id, customerId, pin);

        return ResponseEntity.ok(ApiResponse.success(null, "Invoice paid successfully via wallet!"));
    }
}