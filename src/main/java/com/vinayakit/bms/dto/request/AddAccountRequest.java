package com.vinayakit.bms.dto.request;

import com.vinayakit.bms.entity.Account;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddAccountRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    @NotNull(message = "Currency is required")
    private String currency;

}