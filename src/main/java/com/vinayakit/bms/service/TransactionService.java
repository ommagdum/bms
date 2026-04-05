package com.vinayakit.bms.service;

import com.vinayakit.bms.dto.request.DepositWithdrawalRequest;
import com.vinayakit.bms.dto.request.FundTransferRequest;
import com.vinayakit.bms.dto.response.ReceiptResponse;
import com.vinayakit.bms.dto.response.TransferResponse;
import com.vinayakit.bms.entity.Account;
import com.vinayakit.bms.entity.Transaction;
import com.vinayakit.bms.exception.AccountStatusException;
import com.vinayakit.bms.exception.InsufficientFundsException;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.AccountRepository;
import com.vinayakit.bms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FraudDetectionService fraudDetectionService;

    @Transactional
    public TransferResponse transfer(FundTransferRequest request) {

        fraudDetectionService.runAllChecks(
                request.getSourceAccountId(),
                request.getAmount(),
                request.getIpAddress()
        );

        // Lock accounts in UUID order to prevent deadlocks
        List<UUID> ordered = List.of(request.getSourceAccountId(),
                request.getTargetAccountId())
                .stream()
                .sorted()
                .toList();

        Account first = accountRepository.findByIdForUpdate(ordered.get(0))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + ordered.get(0)
                ));
        Account second = accountRepository.findByIdForUpdate(ordered.get(1))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + ordered.get(1)
                ));

        Account source = first.getId().equals(request.getSourceAccountId())
                ? first : second;
        Account target = first.getId().equals(request.getTargetAccountId())
                ? first : second;

        validateAccountActive(source);
        validateAccountActive(target);

        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + source.getBalance()
            );
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        target.setBalance(target.getBalance().add(request.getAmount()));
        accountRepository.save(source);
        accountRepository.save(target);

        Transaction debit = new Transaction();
        debit.setAccount(source);
        debit.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        debit.setAmount(request.getAmount());
        debit.setBalanceAfter(source.getBalance());
        debit.setDescription(request.getDescription() != null ? request.getDescription() : "Transfer to " + target.getAccountNumber());
        debit.setStatus(Transaction.TransactionStatus.COMPLETED);
        debit.setIpAddress(request.getIpAddress());
        transactionRepository.save(debit);

        Transaction credit = new Transaction();
        credit.setAccount(target);
        credit.setTransactionType(Transaction.TransactionType.DEPOSIT);
        credit.setAmount(request.getAmount());
        credit.setBalanceAfter(target.getBalance());
        credit.setDescription("Transfer from " + source.getAccountNumber());
        credit.setStatus(Transaction.TransactionStatus.COMPLETED);
        credit.setIpAddress(request.getIpAddress());
        transactionRepository.save(credit);

        log.info("Transfer completed: {} -> {} amount: {}",
                source.getAccountNumber(),
                target.getAccountNumber(),
                request.getAmount());

        return TransferResponse.builder()
                .transactionId(debit.getId())
                .sourceAccountNumber(source.getAccountNumber())
                .targetAccountNumber(target.getAccountNumber())
                .amount(request.getAmount())
                .status(Transaction.TransactionStatus.COMPLETED)
                .timestamp(debit.getCreatedAt())
                .build();
    }

    @Transactional
    public ReceiptResponse deposit(DepositWithdrawalRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found:" + request.getAccountId()
                ));

        validateAccountActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Deposit");
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setIpAddress(request.getIpAddress());
        transactionRepository.save(transaction);

        log.info("Deposit completed: account {} amount: {}",
                account.getAccountNumber(), request.getAmount());

        return mapToReceipt(transaction);
    }
    
    @Transactional
    public ReceiptResponse withdraw(DepositWithdrawalRequest request) {

        fraudDetectionService.runAllChecks(
                request.getAccountId(),
                request.getAmount(),
                request.getIpAddress()
        );

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found:" + request.getAccountId()
                ));
        
        validateAccountActive(account);
        
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + account.getBalance()
            );
        }
        
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceAfter(account.getBalance());
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Withdrawal");
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction.setIpAddress(request.getIpAddress());
        transactionRepository.save(transaction);

        log.info("Withdrawal completed: account {} amount: {}",
                account.getAccountNumber(), request.getAmount());

        return mapToReceipt(transaction);
    }
    
    @Transactional
    public ReceiptResponse rollback(UUID transactionId){
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId
                ));
        
        if (original.getStatus() == Transaction.TransactionStatus.REVERSED) {
            throw new AccountStatusException("Transaction already reversed");
        }
        
        Account account = accountRepository.findByIdForUpdate(
                original.getAccount().getId()
        ).orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        if (original.getTransactionType() == Transaction.TransactionType.DEPOSIT) {
            if (account.getBalance().compareTo(original.getAmount()) < 0) {
                throw new InsufficientFundsException(
                        "Insufficient funds to reverse this deposit"
                );
            }
            account.setBalance(account.getBalance().subtract(original.getAmount()));
        } else if (original.getTransactionType() == Transaction.TransactionType.WITHDRAWAL) {
            account.setBalance(account.getBalance().add(original.getAmount()));
        }
        
        accountRepository.save(account);
        
        original.setStatus(Transaction.TransactionStatus.REVERSED);
        transactionRepository.save(original);
        
        Transaction reversal = new Transaction();
        reversal.setAccount(account);
        reversal.setTransactionType(Transaction.TransactionType.REVERSAL);
        reversal.setAmount(original.getAmount());
        reversal.setBalanceAfter(account.getBalance());
        reversal.setDescription("Reversal of transaction: " + transactionId);
        reversal.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(reversal);

        log.info("Rollback completed for transaction: {}", transactionId);

        return mapToReceipt(reversal);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId
                ));
        return mapToReceipt(transaction);
    }

    // Helper Methods
    private void validateAccountActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountStatusException(
                    "Account " + account.getAccountNumber() + " is " + account.getStatus()
            );
        }
    }

    private ReceiptResponse mapToReceipt(Transaction t) {
        return ReceiptResponse.builder()
                .transactionId(t.getId())
                .type(t.getTransactionType())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .status(t.getStatus())
                .ipAddress(t.getIpAddress())
                .timestamp(t.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ReceiptResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(this::mapToReceipt);
    }
}
