package com.vinayakit.bms.controller;

import com.vinayakit.bms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/balance-sheet")
    public ResponseEntity<byte[]> balanceSheet() throws IOException {
        return buildExcelResponse(
                reportService.generateBalanceSheet(),
                "balance_sheet.xlsx"
        );
    }

    @GetMapping("/profit-loss")
    public ResponseEntity<byte[]> profitLoss() throws IOException {
        return buildExcelResponse(
                reportService.generateProfitLoss(),
                "profit_loss.xlsx"
        );
    }

    @GetMapping("/cash-flow")
    public ResponseEntity<byte[]> cashFlow() throws IOException {
        return buildExcelResponse(
                reportService.generateCashFlow(),
                "cash_flow.xlsx"
        );
    }

    @GetMapping("/compliance")
    public ResponseEntity<byte[]> compliance() throws IOException {
        return buildExcelResponse(
                reportService.generateComplianceReport(),
                "compliance_report.xlsx"
        );
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + filename)
                .header("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(data);
    }
}