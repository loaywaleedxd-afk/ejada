package com.ejada.inventory.dto.response;

import com.ejada.inventory.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        Integer quantityAvailable,
        Boolean active
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getPrice(), p.getImageUrl(), p.getQuantityAvailable(), p.getActive());
    }
}
