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
public class LoanApplicationResponse {

    private UUID loanId;
    private String accountNumber;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal monthlyEmi;
    private Loan.LoanType loanType;
    private Loan.LoanStatus loanStatus;
    private LocalDate appliedAt;
}
