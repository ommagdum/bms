package com.vinayakit.bms.controller;

import com.vinayakit.bms.dto.request.RecurringPaymentRequest;
import com.vinayakit.bms.dto.response.RecurringPaymentResponse;
import com.vinayakit.bms.scheduler.RecurringPaymentScheduler;
import com.vinayakit.bms.service.RecurringPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recurring-payments")
@RequiredArgsConstructor
public class RecurringPaymentController {

    private final RecurringPaymentService recurringPaymentService;
    private final RecurringPaymentScheduler recurringPaymentScheduler;

    @PostMapping
    public ResponseEntity<RecurringPaymentResponse> create(
            @Valid @RequestBody RecurringPaymentRequest request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringPaymentService.createRecurringPayment(request));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<List<RecurringPaymentResponse>> getByAccount(
            @PathVariable UUID accountId
            ) {
        return ResponseEntity.ok(
                recurringPaymentService.getByAccount(accountId)
        );
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RecurringPaymentResponse> cancel(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                recurringPaymentService.cancelRecurringPayment(id)
        );
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerManually() {
        recurringPaymentScheduler.processScheduledPayments();
        return ResponseEntity.ok("Scheduler triggered manually");
    }
}
