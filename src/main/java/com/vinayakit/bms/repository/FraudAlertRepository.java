package com.vinayakit.bms.repository;

import com.vinayakit.bms.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    List<FraudAlert> findByResolvedFalse();
    List<FraudAlert> findByAccountIdAndResolvedFalse(UUID accountId);
}
