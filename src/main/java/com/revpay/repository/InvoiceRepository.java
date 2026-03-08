package com.revpay.repository;

import com.revpay.model.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByBusinessProfile_ProfileId(Long profileId);

    Page<Invoice> findByBusinessProfile_ProfileId(Long profileId, Pageable pageable);

    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    Page<Invoice> findByStatus(Invoice.InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByCustomerEmail(String customerEmail, Pageable pageable);

}