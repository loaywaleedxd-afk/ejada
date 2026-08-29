package com.ejada.shop.dto.response;

import com.ejada.shop.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String category,
        String imageUrl,
        Boolean active
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getPrice(), p.getCategory(), p.getImageUrl(), p.getActive());
    }
}
