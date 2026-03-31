package com.vinayakit.bms.service;

import com.vinayakit.bms.entity.Transaction;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.AccountRepository;
import com.vinayakit.bms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Transactional(readOnly = true)
    public byte[] exportToExcel(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to
    ) throws IOException {

        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountId
                ));

        List<Transaction> transactions = transactionRepository
                .findByAccountIdAndCreatedAtBetween(
                        accountId, from, to, Pageable.unpaged()
                ).getContent();

        try(XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transaction History");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header rows
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "Transaction ID", "Type", "Amount",
                    "Balance After", "Description", "Status",
                    "IP Address", "Date"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Transaction t: transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getId().toString());
                row.createCell(1).setCellValue(t.getTransactionType().toString());
                row.createCell(2).setCellValue(t.getAmount().doubleValue());
                row.createCell(3).setCellValue(t.getBalanceAfter() != null ? t.getBalanceAfter().doubleValue() : 0);
                row.createCell(4).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(5).setCellValue(t.getStatus().toString());
                row.createCell(6).setCellValue(t.getIpAddress() != null ? t.getIpAddress() : "");
                row.createCell(7).setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().format(FORMATTER) : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            log.info("Excel export generated for account: {} transactions: {}",
                    accountId, transactions.size());
            return outputStream.toByteArray();
        }
    }
}
