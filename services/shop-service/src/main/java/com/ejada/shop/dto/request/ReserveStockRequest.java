package com.ejada.shop.dto.request;

public record ReserveStockRequest(
        String sku,
        Integer quantity,
        String orderRef
) {
}
