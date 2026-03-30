package com.vinayakit.bms.repository;

import com.vinayakit.bms.entity.RecurringPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringPaymentRepository extends JpaRepository<RecurringPayment, UUID> {
    List<RecurringPayment> findByNextRunDateLessThanEqualAndStatus(
            LocalDate date,
            RecurringPayment.PaymentStatus status
    );

    List<RecurringPayment> findBySourceAccountId(UUID accountId);
}
