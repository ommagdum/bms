package com.vinayakit.bms.repository;

import com.vinayakit.bms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.account.id = :accountId
            AND t.createdAt BETWEEN :from AND :to
            AND (:type IS NULL OR t.transactionType = :type)
            AND t.amount BETWEEN :minAmount AND :maxAmount
            """)
    Page<Transaction> findWithFilters(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("type") Transaction.TransactionType type,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.account.id = :accountId
            AND t.createdAt >= :since
            """)
    long countRecentTransactions(
            @Param("accountId") UUID accountId,
            @Param("since") LocalDateTime since
    );

    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.ipAddress = :ipAddress
            AND t.createdAt >= :since
            """)
    long countRecentTransactionsByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );
}
