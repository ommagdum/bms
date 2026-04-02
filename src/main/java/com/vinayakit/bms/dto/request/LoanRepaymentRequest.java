package com.vinayakit.bms.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRepaymentRequest {

    @NotNull(message = "Repayment amount is required")
    @DecimalMin(value = "1.00", message = "Repayment amount must be at least 1")
    private BigDecimal amount;

}
