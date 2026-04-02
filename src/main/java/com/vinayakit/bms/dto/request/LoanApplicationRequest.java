package com.vinayakit.bms.dto.request;

import com.vinayakit.bms.entity.Loan;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplicationRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1000")
    private BigDecimal loanAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.01", message = "Interest rate must be greater than 0")
    @DecimalMax(value = "99.99", message = "Interest rate must be less than 100")
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    @Max(value = 360, message = "Maximum tenure is 360 month (30 years)")
    private Integer tenureMonth;

    @NotNull(message = "Loan type is required")
    private Loan.LoanType loanType;
}
