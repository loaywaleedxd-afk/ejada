package com.ejada.shop.dto.response;

public record ReservationResponse(
        String sku,
        String status,
        Integer quantityAvailable
) {
}
