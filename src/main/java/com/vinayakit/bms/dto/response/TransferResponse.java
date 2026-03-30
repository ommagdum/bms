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
public class TransferResponse {
    private UUID transactionId;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private Transaction.TransactionStatus status;
    private LocalDateTime timestamp;
}
