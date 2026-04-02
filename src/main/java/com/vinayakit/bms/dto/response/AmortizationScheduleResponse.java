package com.vinayakit.bms.dto.response;

import com.vinayakit.bms.entity.Loan;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AmortizationScheduleResponse {

    private UUID loanId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Loan.LoanType loanType;
    private BigDecimal monthlyEmi;
    private BigDecimal totalPayable;
    private BigDecimal totalInterest;
    private List<AmortizationEntry> schedule;
}
