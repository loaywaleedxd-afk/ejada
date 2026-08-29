package com.ejada.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DebitRequest(
        @NotNull Long userId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull String reference,
        String orderRef
) {
}
