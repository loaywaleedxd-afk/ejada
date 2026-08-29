package com.ejada.shop.dto.response;

import com.ejada.shop.domain.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        Long userId,
        CartStatus status,
        List<Item> items,
        BigDecimal total
) {
    public record Item(
            Long productId,
            String name,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
