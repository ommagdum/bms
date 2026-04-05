package com.vinayakit.bms.controller;

import com.vinayakit.bms.dto.request.DepositWithdrawalRequest;
import com.vinayakit.bms.dto.request.FundTransferRequest;
import com.vinayakit.bms.dto.response.ReceiptResponse;
import com.vinayakit.bms.dto.response.TransferResponse;
import com.vinayakit.bms.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody FundTransferRequest request
            ) {
        return ResponseEntity.ok(transactionService.transfer(request));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ReceiptResponse> deposit(
            @Valid @RequestBody DepositWithdrawalRequest request
    ) {
        return ResponseEntity.ok(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ReceiptResponse> withdraw(
            @Valid @RequestBody DepositWithdrawalRequest request
    ) {
        return ResponseEntity.ok(transactionService.withdraw(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{transactionId}/rollback")
    public ResponseEntity<ReceiptResponse> rollback(
            @PathVariable UUID transactionId
            ) {
        return ResponseEntity.ok(transactionService.rollback(transactionId));
    }

    @GetMapping("/{transactionId}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable UUID transactionId
    ) {
        return ResponseEntity.ok(transactionService.getReceipt(transactionId));
    }

}
