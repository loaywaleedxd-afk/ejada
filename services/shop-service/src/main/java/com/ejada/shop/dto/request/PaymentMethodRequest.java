package com.ejada.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 80) String label,
        Boolean walletBacked,
        Boolean active
) {
}
