package com.vinayakit.bms.scheduler;

import com.vinayakit.bms.dto.request.DepositWithdrawalRequest;
import com.vinayakit.bms.entity.Loan;
import com.vinayakit.bms.entity.RecurringPayment;
import com.vinayakit.bms.repository.LoanRepository;
import com.vinayakit.bms.repository.RecurringPaymentRepository;
import com.vinayakit.bms.service.RecurringPaymentService;
import com.vinayakit.bms.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanRepaymentScheduler {

    private static final int GRACE_PERIOD_DAYS = 3;
    private static final BigDecimal PENALTY_RATE = new BigDecimal("0.02"); // 2%

    private final LoanRepository loanRepository;
    private final TransactionService transactionService;

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "LoanRepaymentScheduler_processEmi",
                lockAtMostFor = "PT10M",
                lockAtLeastFor = "PT30M")
    @Transactional
    public void processEmiPayments() {
        LocalDate today = LocalDate.now();
        List<Loan> dueLoans = loanRepository.findDueLoans(today);
        log.info("LoanRepaymentScheduler: found {} due loans for {}", dueLoans.size(), today);

        for (Loan loan : dueLoans) {
            try {
                processLoan(loan, today);
            } catch (Exception e) {
                log.error("Failed to process EMI for loan {}: {}", loan.getId(), e.getMessage());
            }
        }
    }

    public void triggerManually() {
        log.info("LoanRepaymentScheduler: manual trigger initiated");
        processEmiPayments();
    }

    private void processLoan(Loan loan, LocalDate today) {

        // 1. Check if overdue beyond grace period - apply penalty
        if (isOverdue(loan, today)) {
            applyLatePenalty(loan);
        }

        // 2. Determine repayment amount
        // Use EMI or outstanding balance whichever is smaller (final payment)
        BigDecimal repaymentAmount = loan.getMonthlyEmi()
                .min(loan.getOutstandingBalance());

        // 3. Check account balance before attempting debit
        BigDecimal accountBalance = loan.getAccount().getBalance();
        if (accountBalance.compareTo(repaymentAmount) < 0) {
            log.warn("Insufficient funds for loan {}. Marking as DEFAULTED.", loan.getId());
            loan.setLoanStatus(Loan.LoanStatus.DEFAULTED);
            loanRepository.save(loan);
            return;
        }

        // 4. Debit EMI from account
        DepositWithdrawalRequest withdrawalRequest = new DepositWithdrawalRequest();
        withdrawalRequest.setAccountId(loan.getAccount().getId());
        withdrawalRequest.setAmount(repaymentAmount);
        withdrawalRequest.setDescription("Auto EMI Debit - Loan#" + loan.getId());

        transactionService.withdraw(withdrawalRequest);

        // 5. Update outstanding balance
        BigDecimal newBalance = loan.getOutstandingBalance()
                .subtract(repaymentAmount)
                .setScale(2, RoundingMode.HALF_UP);
        loan.setOutstandingBalance(newBalance);

        // 6. Push next payment date forward by 1 month
        loan.setNextPaymentDate(loan.getNextPaymentDate().plusMonths(1));

        // 7. Auto-close if fully paid
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setLoanStatus(Loan.LoanStatus.CLOSED);
            log.info("Loan {} fully repaid and closed.", loan.getId());
        }

        loanRepository.save(loan);
        log.info("EMI of {} processed for loan {}", repaymentAmount, loan.getId());
    }

    private void applyLatePenalty(Loan loan) {
        BigDecimal penalty = loan.getMonthlyEmi().multiply(PENALTY_RATE
                ).setScale(2, RoundingMode.HALF_UP);

        DepositWithdrawalRequest penaltyRequest = new DepositWithdrawalRequest();
        penaltyRequest.setAccountId(loan.getAccount().getId());
        penaltyRequest.setAmount(penalty);
        penaltyRequest.setDescription("Late Payment Penalty - Loan#" + loan.getId());

        transactionService.withdraw(penaltyRequest);

        log.warn("Late penalty of {} applied for loan {}", penalty, loan.getId());
    }

    private boolean isOverdue(Loan loan, LocalDate today) {
        return loan.getNextPaymentDate() != null && loan.getNextPaymentDate().plusDays(GRACE_PERIOD_DAYS)
                .isBefore(today);
    }
}
