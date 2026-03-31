package com.vinayakit.bms.service;

import com.vinayakit.bms.dto.request.FundTransferRequest;
import com.vinayakit.bms.dto.request.RecurringPaymentRequest;
import com.vinayakit.bms.dto.response.RecurringPaymentResponse;
import com.vinayakit.bms.entity.Account;
import com.vinayakit.bms.entity.RecurringPayment;
import com.vinayakit.bms.exception.AccountStatusException;
import com.vinayakit.bms.exception.ResourceNotFoundException;
import com.vinayakit.bms.repository.AccountRepository;
import com.vinayakit.bms.repository.RecurringPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringPaymentService {

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    @Transactional
    public RecurringPaymentResponse createRecurringPayment(
            RecurringPaymentRequest request
    ) {
        Account source = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source account not found"
                ));

        Account target = accountRepository.findById(request.getTargetAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target account not found"
                ));

        if (source.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountStatusException("Source account is not active");
        }

        if (target.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new AccountStatusException("Target account is not active");
        }

        RecurringPayment payment = new RecurringPayment();
        payment.setSourceAccount(source);
        payment.setTargetAccount(target);
        payment.setAmount(request.getAmount());
        payment.setFrequency(request.getFrequency());
        payment.setNextRunDate(request.getStartDate());
        payment.setStatus(RecurringPayment.PaymentStatus.ACTIVE);
        payment.setDescription(request.getDescription());
        recurringPaymentRepository.save(payment);

        log.info("Recurring payment create: {} -> {} every {} amount: {}",
                source.getAccountNumber(),
                target.getAccountNumber(),
                request.getFrequency(),
                request.getAmount());

        return mapToResponse(payment);
    }

    @Transactional
    public void processRecurringPayment(RecurringPayment payment) {
        try {
            FundTransferRequest transferRequest = new FundTransferRequest();
            transferRequest.setSourceAccountId(
                    payment.getSourceAccount().getId());
            transferRequest.setTargetAccountId(
                    payment.getTargetAccount().getId());
            transferRequest.setAmount(payment.getAmount());
            transferRequest.setDescription(payment.getDescription() != null
                    ? payment.getDescription() : "Recurring payment");
            transferRequest.setIpAddress("SYSTEM");

            transactionService.transfer(transferRequest);

            payment.setNextRunDate(calculateNextRunDate(
                    payment.getNextRunDate(), payment.getFrequency()));
            payment.setFailureReason(null);
            recurringPaymentRepository.save(payment);

            log.info("Recurring payment processed: id={} nextRun={}",
                    payment.getId(), payment.getNextRunDate());

        } catch (Exception e) {
            log.error("Recurring payment failed: id={} reason={}",
                    payment.getId(), e.getMessage());
            payment.setStatus(RecurringPayment.PaymentStatus.PAUSED);
            payment.setFailureReason(e.getMessage());
            recurringPaymentRepository.save(payment);
        }
    }

    @Transactional
    public RecurringPaymentResponse cancelRecurringPayment(UUID id) {
        RecurringPayment payment = recurringPaymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recurring payment not found: " + id
                ));
        payment.setStatus(RecurringPayment.PaymentStatus.CANCELLED);
        recurringPaymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Transactional
    public List<RecurringPaymentResponse> getByAccount(UUID accountId) {
        return recurringPaymentRepository
                .findBySourceAccountId(accountId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Helper Methods
    private LocalDate calculateNextRunDate(LocalDate current, RecurringPayment.Frequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    private RecurringPaymentResponse mapToResponse(RecurringPayment payment) {
        return RecurringPaymentResponse.builder()
                .id(payment.getId())
                .sourceAccountId(payment.getSourceAccount().getId())
                .targetAccountId(payment.getTargetAccount().getId())
                .amount(payment.getAmount())
                .frequency(payment.getFrequency())
                .nextRunDate(payment.getNextRunDate())
                .status(payment.getStatus())
                .description(payment.getDescription())
                .build();
    }
}
