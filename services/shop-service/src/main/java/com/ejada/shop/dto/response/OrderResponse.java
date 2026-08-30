package com.ejada.shop.dto.response;

import com.ejada.shop.domain.OrderStatus;
import com.ejada.shop.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        Long userId,
        OrderStatus status,
        BigDecimal subtotal,
        Integer discountPercent,
        String discountCode,
        BigDecimal totalAmount,
        String paymentMethod,
        LocalDateTime createdAt,
        List<Item> items,
        Payment payment
) {
    public record Item(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    public record Payment(
            Long id,
            BigDecimal amount,
            String method,
            PaymentStatus status
    ) {
    }
}
