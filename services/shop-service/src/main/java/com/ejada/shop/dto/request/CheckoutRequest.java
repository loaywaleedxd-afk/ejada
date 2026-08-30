package com.ejada.shop.dto.request;

public record CheckoutRequest(
        String paymentMethodCode,
        String discountCode
) {
}
