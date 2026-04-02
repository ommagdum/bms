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
public class LoanStatusResponse {

    private UUID loanId;
    private String accountNumber;
    private BigDecimal loanAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal monthlyEmi;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private Loan.LoanType loanType;
    private Loan.LoanStatus loanStatus;
    private String approvedBy;
    private LocalDate approvedAt;
    private LocalDate disbursedAt;
    private LocalDate nextPaymentDate;
    private String rejectionReason;
}
