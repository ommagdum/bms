package com.vinayakit.bms.service;

import com.vinayakit.bms.entity.Account;
import com.vinayakit.bms.entity.FraudAlert;
import com.vinayakit.bms.exception.FraudSuspectedException;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.AccountRepository;
import com.vinayakit.bms.repository.FraudAlertRepository;
import com.vinayakit.bms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AccountRepository accountRepository;

    private static final int VELOCITY_LIMIT = 5;
    private static final int IP_VELOCITY_LIMIT = 10;
    private static final int RAPID_TRANSFER_LIMIT = 3;
    private static final BigDecimal AMOUNT_LIMIT = new BigDecimal("100000");

    @Transactional
    public void runAllChecks(UUID accountId, BigDecimal amount, String ipAddress) {
        checkVelocity(accountId);
        checkAmountLimit(accountId, amount);
        checkRapidTransfers(accountId);
        if (ipAddress != null && !ipAddress.equals("SYSTEM")) {
            checkIpVelocity(accountId, ipAddress);
        }
    }

    @Transactional
    public void checkIpVelocity(UUID accountId, String ipAddress) {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        long count = transactionRepository
                .countRecentTransactionsByIp(ipAddress, tenMinutesAgo);

        if (count >= IP_VELOCITY_LIMIT) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
            createAlert(account, null, FraudAlert.AlertType.SUSPICIOUS_IP,
                    "IP "+ ipAddress + " made " + count + " transactions in 10 minutes", ipAddress);
            log.warn("FRAUD: Suspicious IP: {} count: {}", ipAddress, count);
            throw new FraudSuspectedException(
                    "Transaction blocked: suspicious activity from your IP address"
            );
        }
    }

    @Transactional
    public void checkRapidTransfers(UUID accountId) {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        long count = transactionRepository.countRecentTransactions(accountId, fiveMinutesAgo);

        if (count >= RAPID_TRANSFER_LIMIT) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
            createAlert(account, null, FraudAlert.AlertType.RAPID_TRANSFERS,
                    "Account made " + count + " transfers in 5 minutes", null);
            log.warn("FRAUD: Rapid transfers on account: {}", accountId);
        }
    }

    @Transactional
    public void checkAmountLimit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(AMOUNT_LIMIT) > 0) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
            createAlert(account, null, FraudAlert.AlertType.AMOUNT_LIMIT,
                    "Large transaction flagged: amount=" + amount + " exceeds limit=" + AMOUNT_LIMIT, null);
            log.warn("FRAUD: Large amount flagged on account: {} amount: {}",
                    accountId, amount);
        }
    }

    @Transactional
    public void checkVelocity(UUID accountId) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long count = transactionRepository
                .countRecentTransactions(accountId, oneMinuteAgo);

        if (count >= VELOCITY_LIMIT) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
            createAlert(account, null, FraudAlert.AlertType.VELOCITY_BREACH, "Account exceeded " + VELOCITY_LIMIT + " transactions in 1 minute. Count: " + count, null);
            log.warn("FRAUD: Velocity breach on account: {}", accountId);
            throw new FraudSuspectedException(
                    "Transaction blocked: too many transactions in a short period"
            );
        }
    }

    @Transactional
    public FraudAlert resolveAlert(UUID alertId){
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alert not found: " + alertId
                ));
        alert.setResolved(true);
        return fraudAlertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> getAllUnresolved() {
        return fraudAlertRepository.findByResolvedFalse();
    }

    @Transactional(readOnly = true)
    public List<FraudAlert> getByAccount(UUID accountId) {
        return fraudAlertRepository.findByAccountIdAndResolvedFalse(accountId);
    }

    private void createAlert(Account account, UUID transactionId, FraudAlert.AlertType alertType, String description, String ipAddress) {
        FraudAlert alert = new FraudAlert();
        alert.setAccount(account);
        alert.setTransactionId(transactionId);
        alert.setAlertType(alertType);
        alert.setDescription(description);
        alert.setIpAddress(ipAddress);
        alert.setResolved(false);
        fraudAlertRepository.save(alert);
        log.warn("FRAUD ALERT created: type={} account={} desc={}", alertType, account.getId(), description);
    }
}
