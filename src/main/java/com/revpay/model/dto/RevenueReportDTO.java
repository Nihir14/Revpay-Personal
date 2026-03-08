package com.revpay.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {

    private String period;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount;

    private long transactionCount;

    private double growthPercentage;

    private String currency;
}