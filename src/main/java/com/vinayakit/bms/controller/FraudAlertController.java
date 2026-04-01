package com.vinayakit.bms.controller;

import com.vinayakit.bms.entity.FraudAlert;
import com.vinayakit.bms.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudAlertController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping("/alerts")
    public ResponseEntity<List<FraudAlert>> getAllUnresolved() {
        return ResponseEntity.ok(fraudDetectionService.getAllUnresolved());
    }

    @GetMapping("/alerts/{accountId}")
    public ResponseEntity<List<FraudAlert>> getByAccount(
            @PathVariable UUID accountId
            ) {
        return ResponseEntity.ok(
                fraudDetectionService.getByAccount(accountId)
        );
    }

    @PatchMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<FraudAlert> resolve(
            @PathVariable UUID alertId
    ) {
        return ResponseEntity.ok(
                fraudDetectionService.resolveAlert(alertId)
        );
    }
}
