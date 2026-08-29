package com.ejada.shop.dto.request;

import java.math.BigDecimal;

public record InventoryProductRequest(
        String sku,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Integer quantityAvailable,
        Boolean active
) {
}
