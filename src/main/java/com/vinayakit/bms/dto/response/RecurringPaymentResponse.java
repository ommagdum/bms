package com.vinayakit.bms.dto.response;


import com.vinayakit.bms.entity.RecurringPayment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecurringPaymentResponse {
    private UUID id;
    private UUID sourceAccountId;
    private UUID targetAccountId;
    private BigDecimal amount;
    private RecurringPayment.Frequency frequency;
    private LocalDate nextRunDate;
    private RecurringPayment.PaymentStatus status;
    private String description;
}
