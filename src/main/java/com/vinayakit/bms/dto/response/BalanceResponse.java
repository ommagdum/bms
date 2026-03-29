package com.vinayakit.bms.dto.response;

import com.vinayakit.bms.entity.Account;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalanceResponse {
    private UUID accountId;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private Account.AccountStatus status;
}
