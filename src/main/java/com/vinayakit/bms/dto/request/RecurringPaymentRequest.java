package com.vinayakit.bms.dto.request;

import com.vinayakit.bms.entity.RecurringPayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringPaymentRequest {

    @NotNull(message = "Source account is required")
    private UUID sourceAccountId;

    @NotNull(message = "Target account is required")
    private UUID targetAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Frequency is required")
    private RecurringPayment.Frequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private String description;
}
