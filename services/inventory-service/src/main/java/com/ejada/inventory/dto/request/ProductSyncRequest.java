package com.ejada.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductSyncRequest(
        @NotBlank @Size(max = 40) String sku,
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal price,
        @Size(max = 255) String imageUrl,
        @PositiveOrZero Integer quantityAvailable,
        Boolean active
) {
}
