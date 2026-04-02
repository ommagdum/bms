package com.vinayakit.bms.repository;

import com.vinayakit.bms.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByCustomerId(UUID customerId);

    List<Loan> findByAccountId(UUID accountId);

    List<Loan> findByLoanStatus(Loan.LoanStatus loanStatus);

    @Query("SELECT l FROM Loan l WHERE l.loanStatus = 'ACTIVE' AND l.nextPaymentDate <= :today")
    List<Loan> findDueLoans(LocalDate today);
}
