package com.vinayakit.bms.repository;

import com.vinayakit.bms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByAccountIdAndCreatedAtBetween(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
