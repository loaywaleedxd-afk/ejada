package com.ejada.shop.controller;

import com.ejada.shop.dto.request.PatchProductRequest;
import com.ejada.shop.dto.request.ProductRequest;
import com.ejada.shop.dto.response.PagedResponse;
import com.ejada.shop.dto.response.ProductResponse;
import com.ejada.shop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PagedResponse<ProductResponse> list(@RequestParam(required = false) String category,
                                               @PageableDefault(size = 20) Pageable pageable) {
        return PagedResponse.from(productService.page(category, pageable), ProductResponse::from);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.categories();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(productService.get(id));
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

    @PatchMapping("/{id}")
    public ProductResponse patch(@PathVariable Long id, @Valid @RequestBody PatchProductRequest req) {
        return ProductResponse.from(productService.patch(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
