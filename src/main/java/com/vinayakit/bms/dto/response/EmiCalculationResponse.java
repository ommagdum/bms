package com.vinayakit.bms.dto.response;

import com.vinayakit.bms.entity.Loan;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmiCalculationResponse {
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Loan.LoanType loanType;
    private BigDecimal monthlyEmi;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;
}
