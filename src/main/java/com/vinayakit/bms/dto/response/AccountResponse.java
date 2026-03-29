package com.vinayakit.bms.dto.response;

import com.vinayakit.bms.entity.Account;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private String customerName;
    private String email;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private String currency;
    private Account.AccountStatus status;
    private LocalDateTime createdAt;
}
