package com.ejada.inventory.controller;

import com.ejada.inventory.dto.request.ReserveRequest;
import com.ejada.inventory.dto.response.ReserveResponse;
import com.ejada.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ProductService productService;

    @PostMapping
    public ReserveResponse reserve(@Valid @RequestBody ReserveRequest req) {
        return productService.reserve(req);
    }

    @PostMapping("/confirm")
    public ReserveResponse confirm(@Valid @RequestBody ReserveRequest req) {
        return productService.confirm(req);
    }

    @PostMapping("/release")
    public ReserveResponse release(@Valid @RequestBody ReserveRequest req) {
        return productService.release(req);
    }
}
