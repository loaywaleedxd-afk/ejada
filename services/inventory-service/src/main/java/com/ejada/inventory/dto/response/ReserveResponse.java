package com.ejada.inventory.dto.response;

import com.ejada.inventory.domain.ReservationStatus;

public record ReserveResponse(
        String sku,
        ReservationStatus status,
        Integer quantityAvailable
) {
}
