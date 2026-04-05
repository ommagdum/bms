package com.vinayakit.bms.service;

import com.vinayakit.bms.entity.*;
import com.vinayakit.bms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final CustomerRepository customerRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] generateBalanceSheet() throws IOException {
        List<Account> accounts = accountRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Balance Sheet");
            CellStyle headerStyle = buildHeaderStyle(workbook);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Account Number", "Customer Name", "Account Type",
                    "Balance", "Currency", "Status"
            };
            applyHeaders(headerRow, columns, headerStyle);

            // Data
            BigDecimal totalAssets = BigDecimal.ZERO;
            int rowNum = 1;
            for (Account a : accounts) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getAccountNumber());
                row.createCell(1).setCellValue(a.getCustomer().getFullName());
                row.createCell(2).setCellValue(a.getAccountType().toString());
                row.createCell(3).setCellValue(a.getBalance().doubleValue());
                row.createCell(4).setCellValue(a.getCurrency());
                row.createCell(5).setCellValue(a.getStatus().toString());
                totalAssets = totalAssets.add(a.getBalance());
            }

            // Summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            summaryRow.createCell(2).setCellValue("TOTAL ASSETS");
            summaryRow.createCell(3).setCellValue(totalAssets.doubleValue());

            autoSize(sheet, columns.length);
            log.info("Balance sheet generated: {} accounts", accounts.size());
            return toBytes(workbook);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateProfitLoss() throws IOException {
        List<Transaction> transactions = transactionRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Profit and Loss");
            CellStyle headerStyle = buildHeaderStyle(workbook);

            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Transaction ID", "Type", "Amount",
                    "Description", "Status", "Date"
            };
            applyHeaders(headerRow, columns, headerStyle);

            // Data + totals
            BigDecimal totalDeposits    = BigDecimal.ZERO;
            BigDecimal totalWithdrawals = BigDecimal.ZERO;
            BigDecimal totalFees        = BigDecimal.ZERO;

            int rowNum = 1;
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getId().toString());
                row.createCell(1).setCellValue(t.getTransactionType().toString());
                row.createCell(2).setCellValue(t.getAmount().doubleValue());
                row.createCell(3).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(4).setCellValue(t.getStatus().toString());
                row.createCell(5).setCellValue(t.getCreatedAt() != null
                        ? t.getCreatedAt().format(FORMATTER) : "");

                if (t.getTransactionType() == Transaction.TransactionType.DEPOSIT) {
                    totalDeposits = totalDeposits.add(t.getAmount());
                } else if (t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL) {
                    totalWithdrawals = totalWithdrawals.add(t.getAmount());
                } else if (t.getTransactionType() == Transaction.TransactionType.FEE) {
                    totalFees = totalFees.add(t.getAmount());
                }
            }

            // Summary
            int summaryStart = rowNum + 1;
            sheet.createRow(summaryStart).createCell(0).setCellValue("Total Deposits");
            sheet.getRow(summaryStart).createCell(1).setCellValue(totalDeposits.doubleValue());
            sheet.createRow(summaryStart + 1).createCell(0).setCellValue("Total Withdrawals");
            sheet.getRow(summaryStart + 1).createCell(1).setCellValue(totalWithdrawals.doubleValue());
            sheet.createRow(summaryStart + 2).createCell(0).setCellValue("Total Fees Collected");
            sheet.getRow(summaryStart + 2).createCell(1).setCellValue(totalFees.doubleValue());
            sheet.createRow(summaryStart + 3).createCell(0).setCellValue("Net P&L");
            sheet.getRow(summaryStart + 3).createCell(1)
                    .setCellValue(totalDeposits.subtract(totalWithdrawals).doubleValue());

            autoSize(sheet, columns.length);
            log.info("P&L report generated: {} transactions", transactions.size());
            return toBytes(workbook);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateCashFlow() throws IOException {
        List<Transaction> transactions = transactionRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cash Flow");
            CellStyle headerStyle = buildHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Date", "Transaction ID", "Type",
                    "Amount", "Balance After", "Status"
            };
            applyHeaders(headerRow, columns, headerStyle);

            int rowNum = 1;
            BigDecimal netCashFlow = BigDecimal.ZERO;

            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getCreatedAt() != null
                        ? t.getCreatedAt().format(FORMATTER) : "");
                row.createCell(1).setCellValue(t.getId().toString());
                row.createCell(2).setCellValue(t.getTransactionType().toString());
                row.createCell(3).setCellValue(t.getAmount().doubleValue());
                row.createCell(4).setCellValue(t.getBalanceAfter() != null
                        ? t.getBalanceAfter().doubleValue() : 0);
                row.createCell(5).setCellValue(t.getStatus().toString());

                // Inflow = DEPOSIT, Outflow = WITHDRAWAL/FEE
                if (t.getTransactionType() == Transaction.TransactionType.DEPOSIT) {
                    netCashFlow = netCashFlow.add(t.getAmount());
                } else if (t.getTransactionType() == Transaction.TransactionType.WITHDRAWAL
                        || t.getTransactionType() == Transaction.TransactionType.FEE) {
                    netCashFlow = netCashFlow.subtract(t.getAmount());
                }
            }

            // Net cash flow summary
            Row summaryRow = sheet.createRow(rowNum + 1);
            summaryRow.createCell(2).setCellValue("NET CASH FLOW");
            summaryRow.createCell(3).setCellValue(netCashFlow.doubleValue());

            autoSize(sheet, columns.length);
            log.info("Cash flow report generated: {} transactions", transactions.size());
            return toBytes(workbook);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateComplianceReport() throws IOException {
        List<Customer> customers   = customerRepository.findAll();
        List<FraudAlert> alerts    = fraudAlertRepository.findAll();
        List<Loan> loans           = loanRepository.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = buildHeaderStyle(workbook);

            // Sheet 1 — KYC Status
            Sheet kycSheet = workbook.createSheet("KYC Status");
            Row kycHeader = kycSheet.createRow(0);
            String[] kycColumns = { "Customer ID", "Full Name", "Email", "KYC Status", "Role" };
            applyHeaders(kycHeader, kycColumns, headerStyle);

            int rowNum = 1;
            for (Customer c : customers) {
                Row row = kycSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(c.getId().toString());
                row.createCell(1).setCellValue(c.getFullName());
                row.createCell(2).setCellValue(c.getEmail());
                row.createCell(3).setCellValue(c.getKycStatus().toString());
                row.createCell(4).setCellValue(c.getUserRole() != null
                        ? c.getUserRole().toString() : "");
            }
            autoSize(kycSheet, kycColumns.length);

            // Sheet 2 — Fraud Alerts
            Sheet fraudSheet = workbook.createSheet("Fraud Alerts");
            Row fraudHeader = fraudSheet.createRow(0);
            String[] fraudColumns = {
                    "Alert ID", "Account ID", "Alert Type",
                    "Description", "IP Address", "Resolved", "Date"
            };
            applyHeaders(fraudHeader, fraudColumns, headerStyle);

            rowNum = 1;
            for (FraudAlert f : alerts) {
                Row row = fraudSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(f.getId().toString());
                row.createCell(1).setCellValue(f.getAccount().getId().toString());
                row.createCell(2).setCellValue(f.getAlertType().toString());
                row.createCell(3).setCellValue(f.getDescription() != null
                        ? f.getDescription() : "");
                row.createCell(4).setCellValue(f.getIpAddress() != null
                        ? f.getIpAddress() : "");
                row.createCell(5).setCellValue(f.isResolved() ? "YES" : "NO");
                row.createCell(6).setCellValue(f.getCreatedAt() != null
                        ? f.getCreatedAt().format(FORMATTER) : "");
            }
            autoSize(fraudSheet, fraudColumns.length);

            // Sheet 3 — Loan Status
            Sheet loanSheet = workbook.createSheet("Loan Status");
            Row loanHeader = loanSheet.createRow(0);
            String[] loanColumns = {
                    "Loan ID", "Customer", "Amount",
                    "Outstanding", "Status", "Disbursed At"
            };
            applyHeaders(loanHeader, loanColumns, headerStyle);

            rowNum = 1;
            for (Loan l : loans) {
                Row row = loanSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(l.getId().toString());
                row.createCell(1).setCellValue(l.getCustomer().getFullName());
                row.createCell(2).setCellValue(l.getLoanAmount().doubleValue());
                row.createCell(3).setCellValue(l.getOutstandingBalance().doubleValue());
                row.createCell(4).setCellValue(l.getLoanStatus().toString());
                row.createCell(5).setCellValue(l.getDisbursedAt() != null
                        ? l.getDisbursedAt().toString() : "Not Disbursed");
            }
            autoSize(loanSheet, loanColumns.length);

            log.info("Compliance report generated: {} customers, {} alerts, {} loans",
                    customers.size(), alerts.size(), loans.size());
            return toBytes(workbook);
        }
    }

    private CellStyle buildHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void applyHeaders(Row row, String[] columns, CellStyle style) {
        for (int i = 0; i < columns.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}