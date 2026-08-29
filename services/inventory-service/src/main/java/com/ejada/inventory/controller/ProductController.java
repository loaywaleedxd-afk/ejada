package com.ejada.inventory.controller;

import com.ejada.inventory.dto.request.ProductRequest;
import com.ejada.inventory.dto.request.ProductSyncRequest;
import com.ejada.inventory.dto.response.ProductResponse;
import com.ejada.inventory.dto.request.StockAdjustRequest;
import com.ejada.inventory.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> list() {
        return productService.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(productService.get(id));
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse getBySku(@PathVariable String sku) {
        return ProductResponse.from(productService.getBySku(sku));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductResponse.from(productService.create(req)));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return ProductResponse.from(productService.update(id, req));
    }

    @PostMapping("/{id}/stock")
    public ProductResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest req) {
        return ProductResponse.from(productService.adjustStock(id, req.delta()));
    }

    @PostMapping("/sync")
    public ProductResponse sync(@Valid @RequestBody ProductSyncRequest req) {
        return ProductResponse.from(productService.sync(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
