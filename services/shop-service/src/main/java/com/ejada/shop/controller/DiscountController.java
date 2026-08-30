package com.ejada.shop.controller;

import com.ejada.shop.dto.request.DiscountRequest;
import com.ejada.shop.dto.response.DiscountResponse;
import com.ejada.shop.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService service;

    @GetMapping
    public List<DiscountResponse> list() {
        return service.findAll().stream().map(DiscountResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(DiscountResponse.from(service.create(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
