package com.ejada.shop.controller;

import com.ejada.shop.dto.request.PaymentMethodRequest;
import com.ejada.shop.dto.response.PaymentMethodResponse;
import com.ejada.shop.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService service;

    @GetMapping
    public List<PaymentMethodResponse> list() {
        return service.findActive().stream().map(PaymentMethodResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody PaymentMethodRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentMethodResponse.from(service.create(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
