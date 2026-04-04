package com.vinayakit.bms.service;

import com.vinayakit.bms.entity.Account;
import com.vinayakit.bms.entity.Customer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class CreditScoreService {

    private static final int MINIMUM_SCORE = 600;
    private static final int MAX_SCORE = 900;

    public int calculateScore(Customer customer, Account account) {
        int score = 0;

        score += scoreFromBalance(account.getBalance());
        score += scoreFromAccountAge(account.getCreatedAt());
        score += scoreFromKyc(customer.getKycStatus());

        return Math.min(score, MAX_SCORE);
    }

    public boolean isEligible(Customer customer, Account account) {
        return calculateScore(customer, account) >= MINIMUM_SCORE;
    }

    public String getRejectionReason(Customer customer, Account account) {
        int score = calculateScore(customer, account);

        if (score >= MINIMUM_SCORE) {
            return null; // no rejection
        }

        // Give specific reason based on which factor failed most
        if (customer.getKycStatus() != Customer.KycStatus.VERIFIED) {
            return "KYC not verified. Please complete your KYC before applying for a loan.";
        }

        if (account.getBalance().compareTo(BigDecimal.valueOf(1000)) < 0) {
            return "Insufficient account balance. Minimum balance of ₹1,000 required.";
        }

        return "Credit score too low (" + score + "/" + MAX_SCORE + ")." +
                "Maintain a healthy account balance and ensure KYC is verified.";
    }

    private int scoreFromAccountAge(LocalDateTime createdAt) {
        // Max 300 points based on account age in months
        if (createdAt == null) return 0;

        long months = ChronoUnit.MONTHS.between(createdAt, LocalDateTime.now());

        if (months >= 24) return 300; // 2+ years
        if (months >= 12) return 200; // 1+ years
        if (months >= 6) return 100; // 6+ months
        if (months >= 1) return 50; // 1+ month
        return 0;
    }

    private int scoreFromBalance(BigDecimal balance) {
        // Max 400 points based on balance
        if (balance.compareTo(BigDecimal.valueOf(100000)) >= 0) return 400; // 1L+
        if (balance.compareTo(BigDecimal.valueOf(50000)) >= 0) return 300; // 50K+
        if (balance.compareTo(BigDecimal.valueOf(10000)) >= 0) return 200; // 10K+
        if (balance.compareTo(BigDecimal.valueOf(1000)) >= 0) return 100; // 1K+
        return 0;
    }

    private int scoreFromKyc(Customer.KycStatus kycStatus) {
        // Max 200 points - only VERIFIED gets full points
        return kycStatus == Customer.KycStatus.VERIFIED ? 200 : 0;
    }
}
