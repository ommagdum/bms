package com.vinayakit.bms.dto.response;

import com.vinayakit.bms.entity.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    private UUID id;
    private Transaction.TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private Transaction.TransactionStatus status;
    private LocalDateTime createdAt;
}
