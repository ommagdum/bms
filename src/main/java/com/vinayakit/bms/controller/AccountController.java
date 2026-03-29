package com.vinayakit.bms.controller;

import com.vinayakit.bms.dto.request.CreateAccountRequest;
import com.vinayakit.bms.dto.response.AccountResponse;
import com.vinayakit.bms.dto.response.BalanceResponse;
import com.vinayakit.bms.dto.response.TransactionResponse;
import com.vinayakit.bms.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID accountId
            ) {
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @GetMapping("/{accountId}/statement")
    public ResponseEntity<Page<TransactionResponse>> getStatement(
            @PathVariable UUID accountId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable
            ) {

        // Default to last 30 days if no dates provided
        if (from == null) from = LocalDateTime.now().minusDays(30);
        if (to == null) to = LocalDateTime.now();

        return ResponseEntity.ok(
                accountService.getStatement(accountId, from, to ,pageable)
        );
    }

    @PatchMapping("/{accountId}/freeze")
    public ResponseEntity<AccountResponse> freezeAccount(
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(accountService.freezeAccount(accountId));
    }

    @PatchMapping("/{accountId}/unfreeze")
    public ResponseEntity<AccountResponse> unfreezeAccount(
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(accountService.unfreezeAccount(accountId));
    }

    @PatchMapping("/{accountId}/close")
    public ResponseEntity<AccountResponse> closeAccount(
            @PathVariable UUID accountId
    ) {
        return ResponseEntity.ok(accountService.closeAccount(accountId));
    }
}
