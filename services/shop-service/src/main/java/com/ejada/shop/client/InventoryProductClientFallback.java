package com.ejada.shop.client;

import com.ejada.shop.dto.request.InventoryProductRequest;
import org.springframework.stereotype.Component;

@Component
public class InventoryProductClientFallback implements InventoryProductClient {

    @Override
    public void sync(InventoryProductRequest request) {

    }
}
