package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.model.entity.BusinessProfile;
import com.revpay.model.entity.Invoice;
import com.revpay.model.entity.User;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.InvoiceRepository;
import com.revpay.repository.UserRepository;
import com.revpay.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BusinessProfileRepository businessProfileRepository;

    // --> NEW DEPENDENCIES FOR TRACK 3.5 <--
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;

    // ==============================
    // ADMIN METHODS
    // ==============================

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        validateAdminAccess();
        log.info("ADMIN_ACTION | Fetching all platform invoices");
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getAllInvoicesPaged(Pageable pageable) {
        validateAdminAccess();
        log.info("ADMIN_ACTION | Fetching paginated invoices");
        return invoiceRepository.findAll(pageable);
    }

    // ==============================
    // CREATE INVOICE
    // ==============================

    @Transactional
    public Invoice createInvoice(String userEmail, Invoice invoice) {

        BusinessProfile business = businessProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found for email: " + userEmail));

        if (!business.isVerified()) {
            log.warn("FRAUD_PREVENTION | Unverified business attempted to create invoice. Email: {}", userEmail);
            throw new UnauthorizedException("Your business account is pending admin verification. You cannot issue invoices yet.");
        }

        invoice.setBusinessProfile(business);

        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info("INVOICE_CREATED | ID: {} | Business Profile ID: {}", savedInvoice.getId(), business.getProfileId());
        return savedInvoice;
    }

    // ==============================
    // GET BUSINESS INVOICES
    // ==============================

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoicesByBusiness(Long profileId) {
        getAndValidateBusinessProfile(profileId);
        return invoiceRepository.findByBusinessProfile_ProfileId(profileId);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getAllInvoicesByBusiness(Long profileId, Pageable pageable) {
        getAndValidateBusinessProfile(profileId);
        return invoiceRepository.findByBusinessProfile_ProfileId(profileId, pageable);
    }

    // ==============================
    // SEND INVOICE TO CUSTOMER
    // ==============================

    @Transactional
    public void sendInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        getAndValidateBusinessProfile(invoice.getBusinessProfile().getProfileId());

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be sent.");
        }

        invoice.setStatus(Invoice.InvoiceStatus.SENT);
        invoiceRepository.save(invoice);

        userRepository.findByEmail(invoice.getCustomerEmail()).ifPresentOrElse(
                customer -> {
                    notificationService.createNotification(
                            customer.getUserId(),
                            "You have a new invoice of ₹" + invoice.getTotalAmount() + " from " + invoice.getCustomerName(), // MESSAGE GOES SECOND
                            "INVOICE" // TYPE GOES THIRD
                    );
                    log.info("INVOICE_NOTIFICATION | Sent alert to User ID: {}", customer.getUserId());
                },
                () -> log.warn("INVOICE_SENT | Customer email {} not registered in RevPay yet.", invoice.getCustomerEmail())
        );

        log.info("INVOICE_SENT | Invoice {} status updated to SENT", invoiceId);
    }

    // ==============================
    // PAY INVOICE VIA WALLET
    // ==============================

    @Transactional
    public void payInvoiceViaWallet(Long invoiceId, Long customerUserId, String pin) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already paid.");
        }
        if (invoice.getStatus() != Invoice.InvoiceStatus.SENT) {
            throw new IllegalStateException("Invoice must be SENT before it can be paid.");
        }

        User customer = userRepository.findById(customerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer user not found"));

        if (!customer.getEmail().equalsIgnoreCase(invoice.getCustomerEmail())) {
            log.warn("FRAUD_PREVENTION | User {} attempted to pay invoice belonging to {}", customer.getEmail(), invoice.getCustomerEmail());
            throw new UnauthorizedException("You are not authorized to pay this invoice.");
        }

        // The Magic! Triggers your existing secure logic in WalletService
        walletService.payInvoice(customerUserId, invoiceId, pin);

        log.info("INVOICE_PAID_VIA_WALLET | Invoice {} paid by User {}", invoiceId, customerUserId);
    }

    // ==============================
    // MARK AS PAID (MANUAL / CASH)
    // ==============================

    @Transactional
    public void markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        getAndValidateBusinessProfile(invoice.getBusinessProfile().getProfileId());

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            log.warn("INVOICE_IDEMPOTENCY | Invoice {} is already marked as PAID", invoiceId);
            return;
        }

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        log.info("INVOICE_STATUS_UPDATE | Invoice {} marked as PAID manually", invoiceId);
    }

    // ==============================
    // SECURITY HELPERS
    // ==============================

    private void validateAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized access. Session invalid.");
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            log.warn("UNAUTHORIZED_ACCESS | Non-admin user attempted an admin operation.");
            throw new UnauthorizedException("Admin privileges are required for this action.");
        }
    }

    private BusinessProfile getAndValidateBusinessProfile(Long profileId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Unauthorized access. Session invalid.");
        }

        BusinessProfile profile = businessProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return profile;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetailsImpl userDetails)) {
            throw new UnauthorizedException("Invalid authentication principal structure.");
        }

        Long loggedInUserId = userDetails.getUserId();
        Long ownerUserId = profile.getUser().getUserId();

        if (!loggedInUserId.equals(ownerUserId)) {
            log.warn("UNAUTHORIZED_ACCESS | User {} attempted to access Business Profile {}", loggedInUserId, profileId);
            throw new UnauthorizedException("Access denied. You do not own this business profile.");
        }

        return profile;
    }

    // ==============================
    // GET CUSTOMER INVOICES
    // ==============================

    @Transactional(readOnly = true)
    public Page<Invoice> getMyInvoices(String customerEmail, Pageable pageable) {
        log.info("Fetching invoices for customer email: {}", customerEmail);
        return invoiceRepository.findByCustomerEmail(customerEmail, pageable);
    }
}