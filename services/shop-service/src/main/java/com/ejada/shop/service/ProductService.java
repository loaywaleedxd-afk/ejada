package com.ejada.shop.service;

import com.ejada.shop.client.InventoryProductClient;
import com.ejada.shop.dto.request.InventoryProductRequest;
import com.ejada.shop.dto.request.PatchProductRequest;
import com.ejada.shop.dto.request.ProductRequest;
import com.ejada.shop.entity.Product;
import com.ejada.shop.exception.ConflictException;
import com.ejada.shop.exception.ResourceNotFoundException;
import com.ejada.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;
    private final InventoryProductClient inventoryProducts;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Product> page(Pageable pageable) {
        return products.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    @Transactional
    public Product create(ProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw new ConflictException("SKU already exists: " + req.sku());
        }
        Product saved = products.save(Product.builder()
                .sku(req.sku())
                .name(req.name())
                .description(req.description())
                .price(req.price())
                .category(req.category())
                .imageUrl(req.imageUrl())
                .active(req.active() == null || req.active())
                .build());
        inventoryProducts.sync(new InventoryProductRequest(
                saved.getSku(), saved.getName(), saved.getDescription(), saved.getPrice(),
                saved.getImageUrl(), req.initialStock() == null ? 0 : req.initialStock(), saved.getActive()));
        return saved;
    }

    @Transactional
    public Product update(Long id, ProductRequest req) {
        Product p = get(id);
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setCategory(req.category());
        p.setImageUrl(req.imageUrl());
        if (req.active() != null) p.setActive(req.active());
        return saveAndSync(p);
    }

    @Transactional
    public Product patch(Long id, PatchProductRequest req) {
        Product p = get(id);
        if (req.name() != null) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.price() != null) p.setPrice(req.price());
        if (req.category() != null) p.setCategory(req.category());
        if (req.imageUrl() != null) p.setImageUrl(req.imageUrl());
        if (req.active() != null) p.setActive(req.active());
        return saveAndSync(p);
    }

    @Transactional
    public void delete(Long id) {
        products.delete(get(id));
    }

    private Product saveAndSync(Product p) {
        Product saved = products.save(p);
        inventoryProducts.sync(new InventoryProductRequest(
                saved.getSku(), saved.getName(), saved.getDescription(), saved.getPrice(),
                saved.getImageUrl(), null, saved.getActive()));
        return saved;
    }
}
