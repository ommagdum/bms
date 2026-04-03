package com.vinayakit.bms.service;

import com.vinayakit.bms.dto.response.AmortizationEntry;
import com.vinayakit.bms.dto.response.AmortizationScheduleResponse;
import com.vinayakit.bms.dto.response.EmiCalculationResponse;
import com.vinayakit.bms.entity.Loan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class LoanCalculatorUtil {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public EmiCalculationResponse calculate(BigDecimal principal,
                                            BigDecimal annualRate,
                                            Integer tenureMonths,
                                            Loan.LoanType loanType) {
        BigDecimal emi;

        if (loanType == Loan.LoanType.FLAT) {
            emi = calculateFlatEmi(principal, annualRate, tenureMonths);
        } else {
            emi = calculateReducingEmi(principal, annualRate, tenureMonths);
        }

        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(SCALE, ROUNDING);
        BigDecimal totalInterest = totalPayable.subtract(principal).setScale(SCALE, ROUNDING);

        EmiCalculationResponse response = new EmiCalculationResponse();
        response.setLoanAmount(principal);
        response.setInterestRate(annualRate);
        response.setTenureMonths(tenureMonths);
        response.setLoanType(loanType);
        response.setMonthlyEmi(emi);
        response.setTotalPayable(totalPayable);
        response.setTotalInterest(totalInterest);

        return response;
    }

    public AmortizationScheduleResponse buildSchedule(BigDecimal principal,
                                                      BigDecimal annualRate,
                                                      Integer tenureMonths,
                                                      Loan.LoanType loanType) {
        BigDecimal emi;

        if (loanType == Loan.LoanType.FLAT) {
            emi = calculateFlatEmi(principal, annualRate, tenureMonths);
        } else {
            emi = calculateReducingEmi(principal, annualRate, tenureMonths);
        }

        List<AmortizationEntry> schedule = new ArrayList<>();
        BigDecimal balance = principal.setScale(SCALE, ROUNDING);
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(12), 10, ROUNDING)
                .divide(BigDecimal.valueOf(100), 10, ROUNDING);

        for (int month = 1; month <= tenureMonths; month++) {
            AmortizationEntry entry = new AmortizationEntry();
            entry.setMonth(month);
            entry.setOpeningBalance(balance);
            entry.setEmiAmount(emi);

            BigDecimal interestComponent;
            BigDecimal principalComponent;

            if (loanType == Loan.LoanType.FLAT) {
                // Flat: interest is always same every month
                interestComponent = principal
                        .multiply(annualRate)
                        .divide(BigDecimal.valueOf(100), 10, ROUNDING)
                        .divide(BigDecimal.valueOf(12), SCALE, ROUNDING);
            } else {
                // Reducing: interest based on current outstanding balance
                interestComponent = balance.multiply(monthlyRate)
                        .setScale(SCALE, ROUNDING);
            }

            principalComponent = emi.subtract(interestComponent)
                    .setScale(SCALE, ROUNDING);
            balance = balance.subtract(principalComponent)
                    .setScale(SCALE, ROUNDING);

            // Last month correction - handle rounding leftover
            if (month == tenureMonths && balance.compareTo(BigDecimal.ZERO) != 0) {
                principalComponent = principalComponent.add(balance);
                balance = BigDecimal.ZERO;
            }

            entry.setInterestComponent(interestComponent);
            entry.setPrincipalComponent(principalComponent);
            entry.setClosingBalance(balance.max(BigDecimal.ZERO));

            schedule.add(entry);
        }

        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(tenureMonths))
                .setScale(SCALE, ROUNDING);
        BigDecimal totalInterest = totalPayable.subtract(principal)
                .setScale(SCALE, ROUNDING);

        AmortizationScheduleResponse response = new AmortizationScheduleResponse();
        response.setLoanAmount(principal);
        response.setInterestRate(annualRate);
        response.setTenureMonths(tenureMonths);
        response.setLoanType(loanType);
        response.setMonthlyEmi(emi);
        response.setTotalPayable(totalPayable);
        response.setTotalInterest(totalInterest);
        response.setSchedule(schedule);

        return response;
    }

    private BigDecimal calculateReducingEmi(BigDecimal principal, BigDecimal annualRate, Integer tenureMonths) {
        // r = Annual Rate / 12 / 100
        BigDecimal r = annualRate.divide(BigDecimal.valueOf(12), 10, ROUNDING)
                .divide(BigDecimal.valueOf(100), 10, ROUNDING);

        // (1+r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal onePlusRpowN = onePlusR.pow(tenureMonths, new MathContext(10, ROUNDING));

        // EMI = P x r x (1+r)^n / ((1+r)^n-1)
        return principal
                .multiply(r)
                .multiply(onePlusRpowN)
                .divide(onePlusRpowN.subtract(BigDecimal.ONE), SCALE, ROUNDING);
    }

    private BigDecimal calculateFlatEmi(BigDecimal principal, BigDecimal annualRate, Integer tenureMonths) {
        // Total Interest = P X R X T/12
        BigDecimal totalInterest = principal
                .multiply(annualRate.divide(BigDecimal.valueOf(100), 10, ROUNDING))
                .multiply(BigDecimal.valueOf(tenureMonths)
                        .divide(BigDecimal.valueOf(12), 10, ROUNDING));
        // EMI = (P + Total Interest) / N
        return principal.add(totalInterest)
                .divide(BigDecimal.valueOf(tenureMonths), SCALE, ROUNDING);
    }
}
