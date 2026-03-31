package com.vinayakit.bms.scheduler;

import com.vinayakit.bms.entity.RecurringPayment;
import com.vinayakit.bms.repository.RecurringPaymentRepository;
import com.vinayakit.bms.service.RecurringPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringPaymentScheduler {

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final RecurringPaymentService recurringPaymentService;

    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = "recurringPayments",
                lockAtMostFor = "55s",
                lockAtLeastFor = "50s")
    public void processScheduledPayments() {
        List<RecurringPayment> due = recurringPaymentRepository.findByNextRunDateLessThanEqualAndStatus(
                LocalDate.now(),
                RecurringPayment.PaymentStatus.ACTIVE
        );

        log.info("Scheduler triggered - {} recurring payments due", due.size());

        due.forEach(recurringPaymentService::processRecurringPayment);

        log.info("Scheduler completed - {} payments processed", due.size());
    }
}
