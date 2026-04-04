package com.vinayakit.bms.service;

import com.vinayakit.bms.dto.request.DepositWithdrawalRequest;
import com.vinayakit.bms.dto.request.LoanApplicationRequest;
import com.vinayakit.bms.dto.request.LoanRepaymentRequest;
import com.vinayakit.bms.dto.response.AmortizationScheduleResponse;
import com.vinayakit.bms.dto.response.EmiCalculationResponse;
import com.vinayakit.bms.dto.response.LoanApplicationResponse;
import com.vinayakit.bms.dto.response.LoanStatusResponse;
import com.vinayakit.bms.entity.Account;
import com.vinayakit.bms.entity.Customer;
import com.vinayakit.bms.entity.Loan;
import com.vinayakit.bms.exception.AccountStatusException;
import com.vinayakit.bms.exception.InsufficientFundsException;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.AccountRepository;
import com.vinayakit.bms.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final LoanCalculatorUtil calculatorUtil;
    private final CreditScoreService creditScoreService;
    private final TransactionService transactionService;

    private static final BigDecimal AUTO_APPROVE_LIMIT = BigDecimal.valueOf(100000);

    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request) {

        // 1. Fetch Account and Customer
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        Customer customer = account.getCustomer();

        // 2. Account must be ACTIVE
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountStatusException("Account must be ACTIVE to apply for a loan");
        }

        // 3. Credit score check
        if (!creditScoreService.isEligible(customer, account)) {
            throw new AccountStatusException(
                    creditScoreService.getRejectionReason(customer, account)
            );
        }

        // 4. Calculate EMI
        EmiCalculationResponse emiResult = calculatorUtil.calculate(
                request.getLoanAmount(),
                request.getInterestRate(),
                request.getTenureMonths(),
                request.getLoanType()
        );

        // 5. Create Loan entity
        Loan loan = new Loan();
        loan.setCustomer(customer);
        loan.setAccount(account);
        loan.setLoanAmount(request.getLoanAmount());
        loan.setOutstandingBalance(request.getLoanAmount());
        loan.setInterestRate(request.getInterestRate());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setLoanType(request.getLoanType());
        loan.setMonthlyEmi(emiResult.getMonthlyEmi());
        loan.setLoanStatus(Loan.LoanStatus.PENDING);

        // 6. Auto-approve if below limit
        if (request.getLoanAmount().compareTo(AUTO_APPROVE_LIMIT) < 0) {
            loan.setLoanStatus(Loan.LoanStatus.APPROVED);
            loan.setApprovedBy("SYSTEM");
            loan.setApprovedAt(LocalDate.now());
            disburseLoan(loan, account);
        }

        loanRepository.save(loan);

        return mapToApplicationResponse(loan, account);
    }

    @Transactional
    public LoanStatusResponse approveLoan(UUID loanId, String approvedBy) {
        Loan loan = getLoanOrThrow(loanId);
        if (loan.getLoanStatus() != Loan.LoanStatus.PENDING) {
            throw new AccountStatusException("Only PENDING loans can be approved");
        }

        loan.setLoanStatus(Loan.LoanStatus.APPROVED);
        loan.setApprovedBy(approvedBy);
        loan.setApprovedAt(LocalDate.now());

        disburseLoan(loan, loan.getAccount());
        loanRepository.save(loan);
        return mapToStatusResponse(loan);
    }

    @Transactional
    public LoanStatusResponse rejectLoan(UUID loanId, String reason) {
       Loan loan = getLoanOrThrow(loanId);

       if (loan.getLoanStatus() != Loan.LoanStatus.PENDING) {
           throw new AccountStatusException("Only PENDING loans can be rejected");
       }

       loan.setLoanStatus(Loan.LoanStatus.REJECTED);
       loan.setRejectionReason(reason);
       loanRepository.save(loan);

       return mapToStatusResponse(loan);
    }

    public LoanStatusResponse getLoanById(UUID loanId) {
        return mapToStatusResponse(getLoanOrThrow(loanId));
    }

    public List<LoanStatusResponse> getLoansByCustomer(UUID customerId) {
        return loanRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToStatusResponse)
                .collect(Collectors.toList());
    }

    public EmiCalculationResponse calculateEmi(BigDecimal amount, BigDecimal rate, Integer tenure, Loan.LoanType type) {
        return calculatorUtil.calculate(amount, rate, tenure, type);
    }

    public AmortizationScheduleResponse getSchedule(UUID loanId) {
        Loan loan = getLoanOrThrow(loanId);
        AmortizationScheduleResponse schedule = calculatorUtil.buildSchedule(
                loan.getLoanAmount(),
                loan.getInterestRate(),
                loan.getTenureMonths(),
                loan.getLoanType()
        );
        schedule.setLoanId(loanId);
        return schedule;
    }

    @Transactional
    public LoanStatusResponse repayLoan(UUID loanId, LoanRepaymentRequest request) {

        Loan loan = getLoanOrThrow(loanId);

        if (loan.getLoanStatus() != Loan.LoanStatus.ACTIVE) {
            throw new AccountStatusException("Only ACTIVE loans can be repaid");
        }

        if (request.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
            throw new InsufficientFundsException(
                    "Repayment amount exceeds outstanding balance of " + loan.getOutstandingBalance()
            );
        }

        // Debit from account
        DepositWithdrawalRequest withdrawalRequest = new DepositWithdrawalRequest();
        withdrawalRequest.setAccountId(loan.getAccount().getId());
        withdrawalRequest.setAmount(request.getAmount());
        withdrawalRequest.setDescription("Loan Repayment - Loan#" + loanId);

        transactionService.withdraw(withdrawalRequest);

        // Update outstanding balance
        BigDecimal newBalance = loan.getOutstandingBalance()
                .subtract(request.getAmount());
        loan.setOutstandingBalance(newBalance);

        // Move next payment date forward by 1 month
        if (loan.getNextPaymentDate() != null) {
            loan.setNextPaymentDate(loan.getNextPaymentDate().plusMonths(1));
        }

        // Auto-close if fully paid
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
        }

        loanRepository.save(loan);
        return mapToStatusResponse(loan);
    }

    @Transactional
    public LoanStatusResponse closeLoan(UUID loanId) {

        Loan loan = getLoanOrThrow(loanId);

        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountStatusException(
                    "Cannot close loan. Outstanding balance: "
                    + loan.getOutstandingBalance()
            );
        }

        loan.setLoanStatus(Loan.LoanStatus.CLOSED);
        loanRepository.save(loan);

        return mapToStatusResponse(loan);
    }

    private Loan getLoanOrThrow(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    private void disburseLoan(Loan loan, Account account) {
        // Credit loan amount to account as a DEPOSIT transaction
        DepositWithdrawalRequest depositRequest = new DepositWithdrawalRequest();
        depositRequest.setAccountId(account.getId());
        depositRequest.setAmount(loan.getLoanAmount());
        depositRequest.setDescription("Loan Disbursement - Loan#" + loan.getId());

        transactionService.deposit(depositRequest);

        loan.setLoanStatus(Loan.LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDate.now());
        loan.setNextPaymentDate(LocalDate.now().plusMonths(1));
    }

    private LoanStatusResponse mapToStatusResponse(Loan loan) {
        LoanStatusResponse response = new LoanStatusResponse();
        response.setLoanId(loan.getId());
        response.setAccountNumber(loan.getAccount().getAccountNumber());
        response.setLoanAmount(loan.getLoanAmount());
        response.setOutstandingBalance(loan.getOutstandingBalance());
        response.setMonthlyEmi(loan.getMonthlyEmi());
        response.setInterestRate(loan.getInterestRate());
        response.setTenureMonths(loan.getTenureMonths());
        response.setLoanType(loan.getLoanType());
        response.setLoanStatus(loan.getLoanStatus());
        response.setApprovedBy(loan.getApprovedBy());
        response.setApprovedAt(loan.getApprovedAt());
        response.setDisbursedAt(loan.getDisbursedAt());
        response.setNextPaymentDate(loan.getNextPaymentDate());
        response.setRejectionReason(loan.getRejectionReason());
        return response;
    }

    private LoanApplicationResponse mapToApplicationResponse(Loan loan, Account account) {
        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setLoanId(loan.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setLoanAmount(loan.getLoanAmount());
        response.setInterestRate(loan.getInterestRate());
        response.setTenureMonths(loan.getTenureMonths());
        response.setMonthlyEmi(loan.getMonthlyEmi());
        response.setLoanType(loan.getLoanType());
        response.setLoanStatus(loan.getLoanStatus());
        response.setAppliedAt(loan.getCreatedAt() != null ? loan.getCreatedAt().toLocalDate() : LocalDate.now());
        return response;
    }
}
