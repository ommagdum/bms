package com.vinayakit.bms.service;

import com.vinayakit.bms.dto.request.CreateAccountRequest;
import com.vinayakit.bms.dto.response.AccountResponse;
import com.vinayakit.bms.dto.response.BalanceResponse;
import com.vinayakit.bms.dto.response.TransactionResponse;
import com.vinayakit.bms.entity.Account;
import com.vinayakit.bms.entity.Customer;
import com.vinayakit.bms.entity.Transaction;
import com.vinayakit.bms.exception.AccountStatusException;
import com.vinayakit.bms.exception.DuplicateResourceException;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.AccountRepository;
import com.vinayakit.bms.repository.CustomerRepository;
import com.vinayakit.bms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AccountResponse createAccount(
            CreateAccountRequest request
    ) {

        // Check for duplicate email
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Customer with email " + request.getEmail() + " already exists"
            );
        }

        // Create customer
        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .kycStatus(Customer.KycStatus.PENDING)
                .build();
        customerRepository.save(customer);

        // Create account
        Account account = Account.builder()
                .customer(customer)
                .accountNumber(generateAccountNumber())
                .accountType(request.getAccountType())
                .balance(request.getInitialDeposit())
                .currency(request.getCurrency())
                .status(Account.AccountStatus.ACTIVE)
                .build();
        accountRepository.save(account);

        // Record initial deposit transaction
        if (request.getInitialDeposit().compareTo(java.math.BigDecimal.ZERO) > 0) {
            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
            transaction.setAmount(request.getInitialDeposit());
            transaction.setBalanceAfter(request.getInitialDeposit());
            transaction.setDescription("Initial deposit");
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
        }

        return mapToAccountResponse(account, customer);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId
                ));

        return BalanceResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getStatement(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId
                ));

        return transactionRepository
                .findByAccountIdAndCreatedAtBetween(accountId, from, to, pageable)
                .map(this::maptoTransactionResponse);
    }

    @Transactional
    public AccountResponse freezeAccount(UUID accountId) {
        Account account = getActiveAccount(accountId);
        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);
        return mapToAccountResponse(account, account.getCustomer());
    }

    @Transactional
    public AccountResponse closeAccount(UUID accountId) {
        Account account = getActiveAccount(accountId);

        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new AccountStatusException(
                    "Cannot close account with remaining balance. " +
                            "Please withdraw all funds first."
            );
        }

        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
        return mapToAccountResponse(account, account.getCustomer());
    }

    @Transactional
    public AccountResponse unfreezeAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId
                ));

        if (account.getStatus() != Account.AccountStatus.FROZEN) {
            throw new AccountStatusException("Account is not frozen");
        }

        account.setStatus(Account.AccountStatus.ACTIVE);
        accountRepository.save(account);
        return mapToAccountResponse(account, account.getCustomer());
    }

    // Helper methods

    private Account getActiveAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + accountId
                ));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountStatusException(
                    "Operation not allowed. Account is " + account.getStatus()
            );
        }

        return account;
    }

    private TransactionResponse maptoTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private AccountResponse mapToAccountResponse(Account account, Customer customer) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerName(customer.getFullName())
                .email(customer.getEmail())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }



    private String generateAccountNumber() {
        String number;
        do {
            number = "BMS" +System.currentTimeMillis();
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
