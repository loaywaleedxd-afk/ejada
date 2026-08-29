package com.ejada.shop.client;

import com.ejada.shop.dto.request.InventoryProductRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventoryProductClient", url = "${services.inventory.url}",
        path = "/api/inventory/products", fallback = InventoryProductClientFallback.class)
public interface InventoryProductClient {

    @PostMapping("/sync")
    void sync(@RequestBody InventoryProductRequest request);
}
