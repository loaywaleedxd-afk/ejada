package com.ejada.shop.client;

import com.ejada.shop.dto.response.ReservationResponse;
import com.ejada.shop.dto.request.ReserveStockRequest;
import org.springframework.stereotype.Component;

@Component
public class InventoryClientFallback implements InventoryClient {

    @Override
    public ReservationResponse reserve(ReserveStockRequest request) {
        return new ReservationResponse(request.sku(), "FAILED", null);
    }

    @Override
    public ReservationResponse confirm(ReserveStockRequest request) {
        return new ReservationResponse(request.sku(), "FAILED", null);
    }

    @Override
    public ReservationResponse release(ReserveStockRequest request) {
        return new ReservationResponse(request.sku(), "FAILED", null);
    }
}
