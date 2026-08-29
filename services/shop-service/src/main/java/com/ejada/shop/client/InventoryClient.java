package com.ejada.shop.client;

import com.ejada.shop.dto.response.ReservationResponse;
import com.ejada.shop.dto.request.ReserveStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventoryClient", url = "${services.inventory.url}",
        path = "/api/inventory/reservations", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    @PostMapping
    ReservationResponse reserve(@RequestBody ReserveStockRequest request);

    @PostMapping("/confirm")
    ReservationResponse confirm(@RequestBody ReserveStockRequest request);

    @PostMapping("/release")
    ReservationResponse release(@RequestBody ReserveStockRequest request);
}
