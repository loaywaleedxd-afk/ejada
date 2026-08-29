package com.ejada.shop.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PatchProductRequest(
        @Size(max = 150) String name,
        String description,
        @PositiveOrZero BigDecimal price,
        @Size(max = 80) String category,
        @Size(max = 255) String imageUrl,
        Boolean active
) {
}
