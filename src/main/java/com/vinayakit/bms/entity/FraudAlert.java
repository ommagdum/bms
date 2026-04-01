package com.vinayakit.bms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fraud_alert")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Column(nullable = false)
    private String description;

    @Column
    private String ipAddress;

    @Column(nullable = false)
    private boolean resolved = false;

    public enum AlertType {
        VELOCITY_BREACH, AMOUNT_LIMIT, SUSPICIOUS_IP, RAPID_TRANSFERS
    }
}
