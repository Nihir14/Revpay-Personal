package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.entity.Transaction;
import com.revpay.model.entity.User;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionLedgerService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistoryPaged(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getMyHistoryPaged(User user, String type, Pageable pageable) {
        if (type != null && !type.isEmpty()) {
            try {
                Transaction.TransactionType enumType = Transaction.TransactionType.valueOf(type.toUpperCase());
                return transactionRepository.findByUserAndType(user.getUserId(), enumType, pageable);
            } catch (IllegalArgumentException e) {
                return Page.empty(pageable);
            }
        }
        return transactionRepository.findBySenderOrReceiverOrderByTimestampDesc(user, user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> filterTransactionsPaged(Long userId, String type, LocalDateTime startDate, LocalDateTime endDate, String status, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable) {
        List<Transaction> transactions = getTransactionHistory(userId);

        if (type != null && !type.isEmpty()) {
            transactions = transactions.stream().filter(t -> t.getType().name().equalsIgnoreCase(type)).collect(Collectors.toList());
        }
        if (status != null && !status.isEmpty()) {
            transactions = transactions.stream().filter(t -> t.getStatus().name().equalsIgnoreCase(status)).collect(Collectors.toList());
        }
        if (startDate != null) {
            transactions = transactions.stream().filter(t -> t.getTimestamp().isAfter(startDate)).collect(Collectors.toList());
        }
        if (endDate != null) {
            transactions = transactions.stream().filter(t -> t.getTimestamp().isBefore(endDate)).collect(Collectors.toList());
        }
        if (minAmount != null) {
            transactions = transactions.stream().filter(t -> t.getAmount().compareTo(minAmount) >= 0).collect(Collectors.toList());
        }
        if (maxAmount != null) {
            transactions = transactions.stream().filter(t -> t.getAmount().compareTo(maxAmount) <= 0).collect(Collectors.toList());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), transactions.size());
        List<Transaction> subList = start <= end ? transactions.subList(start, end) : List.of();

        return new PageImpl<>(subList, pageable, transactions.size());
    }

    @Transactional(readOnly = true)
    public Page<Transaction> searchTransactionsPaged(Long userId, String keyword, Pageable pageable) {
        List<Transaction> searched = getTransactionHistory(userId).stream()
                .filter(t -> (t.getTransactionRef() != null && t.getTransactionRef().contains(keyword)) ||
                             (t.getSender() != null && t.getSender().getFullName() != null && t.getSender().getFullName().toLowerCase().contains(keyword.toLowerCase())) ||
                             (t.getReceiver() != null && t.getReceiver().getFullName() != null && t.getReceiver().getFullName().toLowerCase().contains(keyword.toLowerCase())) ||
                             (t.getDescription() != null && t.getDescription().toLowerCase().contains(keyword.toLowerCase())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searched.size());
        List<Transaction> subList = start <= end ? searched.subList(start, end) : List.of();

        return new PageImpl<>(subList, pageable, searched.size());
    }

    @Transactional(readOnly = true)
    public String exportTransactionsToCSV(Long userId) {
        List<Transaction> transactions = getTransactionHistory(userId);
        StringBuilder csv = new StringBuilder("Transaction ID,Date,Type,Amount,Status,Sender,Receiver,Description\n");
        for (Transaction t : transactions) {
            csv.append(t.getTransactionRef()).append(",").append(t.getTimestamp()).append(",")
               .append(t.getType()).append(",").append(t.getAmount()).append(",")
               .append(t.getStatus()).append(",")
               .append(t.getSender() != null ? t.getSender().getFullName() : "N/A").append(",")
               .append(t.getReceiver() != null ? t.getReceiver().getFullName() : "N/A").append(",")
               .append(t.getDescription() != null ? t.getDescription().replace(",", " ") : "").append("\n");
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToPDF(Long userId) {
        List<Transaction> transactions = getTransactionHistory(userId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Transaction History for User: " + userId));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.addCell("TXN ID"); table.addCell("Date"); table.addCell("Type"); table.addCell("Amount");
            table.addCell("Status"); table.addCell("Sender"); table.addCell("Receiver"); table.addCell("Description");

            for (Transaction t : transactions) {
                table.addCell(t.getTransactionRef() != null ? t.getTransactionRef() : "N/A");
                table.addCell(t.getTimestamp() != null ? t.getTimestamp().toString() : "N/A");
                table.addCell(t.getType() != null ? t.getType().toString() : "N/A");
                table.addCell(t.getAmount() != null ? t.getAmount().toString() : "0.00");
                table.addCell(t.getStatus() != null ? t.getStatus().toString() : "N/A");
                table.addCell(t.getSender() != null ? t.getSender().getFullName() : "N/A");
                table.addCell(t.getReceiver() != null ? t.getReceiver().getFullName() : "N/A");
                table.addCell(t.getDescription() != null ? t.getDescription() : "");
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}