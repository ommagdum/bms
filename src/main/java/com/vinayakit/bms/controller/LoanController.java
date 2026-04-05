package com.vinayakit.bms.controller;

import com.vinayakit.bms.dto.request.LoanApplicationRequest;
import com.vinayakit.bms.dto.request.LoanRepaymentRequest;
import com.vinayakit.bms.dto.response.AmortizationScheduleResponse;
import com.vinayakit.bms.dto.response.EmiCalculationResponse;
import com.vinayakit.bms.dto.response.LoanApplicationResponse;
import com.vinayakit.bms.dto.response.LoanStatusResponse;
import com.vinayakit.bms.entity.Loan;
import com.vinayakit.bms.scheduler.LoanRepaymentScheduler;
import com.vinayakit.bms.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;
    private final LoanRepaymentScheduler loanRepaymentScheduler;


    @PostMapping("/apply")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.applyForLoan(request));
    }

    @PostMapping("/calculate-emi")
    public ResponseEntity<EmiCalculationResponse> calculateEmi(
            @RequestParam BigDecimal amount,
            @RequestParam BigDecimal rate,
            @RequestParam Integer tenure,
            @RequestParam Loan.LoanType type
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.calculateEmi(amount, rate, tenure, type));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<LoanStatusResponse> getLoanById(
            @PathVariable UUID loanId
            ) {
        return ResponseEntity.ok(loanService.getLoanById(loanId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoanStatusResponse>> getLoansByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(loanService.getLoansByCustomer(customerId));
    }

    @GetMapping("/{loanId}/schedule")
    public ResponseEntity<AmortizationScheduleResponse> getSchedule(
            @PathVariable UUID loanId
    ) {
        return ResponseEntity.ok(loanService.getSchedule(loanId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{loanId}/approve")
    public ResponseEntity<LoanStatusResponse> approveLoan(
            @PathVariable UUID loanId,
            @RequestParam(defaultValue = "ADMIN") String approvedBy
    ) {
        return ResponseEntity.ok(loanService.approveLoan(loanId, approvedBy));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{loanId}/reject")
    public ResponseEntity<LoanStatusResponse> rejectLoan(
            @PathVariable UUID loanId,
            @RequestParam String reason
    ) {
        return ResponseEntity.ok(loanService.rejectLoan(loanId, reason));
    }

    @PostMapping("/{loanId}/repay")
    public ResponseEntity<LoanStatusResponse> repayLoan(
            @PathVariable UUID loanId,
            @Valid @RequestBody LoanRepaymentRequest request
            ) {
        return ResponseEntity.ok(loanService.repayLoan(loanId, request));
    }

    @PatchMapping("/{loanId}/close")
    public ResponseEntity<LoanStatusResponse> closeLoan(
            @PathVariable UUID loanId
    ) {
        return ResponseEntity.ok(loanService.closeLoan(loanId));
    }

    @PostMapping("/scheduler/trigger")
    public ResponseEntity<String> triggerScheduler() {
        loanRepaymentScheduler.triggerManually();
        return ResponseEntity.ok("Loan repayment scheduler triggered");
    }
}
