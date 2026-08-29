package com.ejada.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReserveRequest(
        @NotBlank String sku,
        @NotNull @Positive Integer quantity,
        String orderRef
) {
}
