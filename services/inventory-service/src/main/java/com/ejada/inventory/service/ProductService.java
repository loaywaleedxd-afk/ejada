package com.ejada.inventory.service;

import com.ejada.inventory.domain.ReservationStatus;
import com.ejada.inventory.dto.request.ProductRequest;
import com.ejada.inventory.dto.request.ProductSyncRequest;
import com.ejada.inventory.dto.request.ReserveRequest;
import com.ejada.inventory.dto.response.ReserveResponse;
import com.ejada.inventory.entity.Product;
import com.ejada.inventory.exception.BusinessRuleException;
import com.ejada.inventory.exception.ConflictException;
import com.ejada.inventory.exception.ResourceNotFoundException;
import com.ejada.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return products.findAll();
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Product getBySku(String sku) {
        return products.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU " + sku + " not found"));
    }

    @Transactional
    public Product create(ProductRequest req) {
        if (products.existsBySku(req.sku())) {
            throw new ConflictException("SKU already exists: " + req.sku());
        }
        return products.save(Product.builder()
                .sku(req.sku())
                .name(req.name())
                .description(req.description())
                .price(req.price())
                .imageUrl(req.imageUrl())
                .quantityAvailable(req.quantityAvailable())
                .active(req.active() == null || req.active())
                .build());
    }

    @Transactional
    public Product sync(ProductSyncRequest req) {
        Product p = products.findBySku(req.sku())
                .orElseGet(() -> Product.builder().sku(req.sku()).quantityAvailable(0).build());
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setImageUrl(req.imageUrl());
        if (req.active() != null) {
            p.setActive(req.active());
        }
        if (req.quantityAvailable() != null) {
            p.setQuantityAvailable(req.quantityAvailable());
        }
        return products.save(p);
    }

    @Transactional
    public Product update(Long id, ProductRequest req) {
        Product p = get(id);
        p.setName(req.name());
        p.setDescription(req.description());
        p.setPrice(req.price());
        p.setImageUrl(req.imageUrl());
        p.setQuantityAvailable(req.quantityAvailable());
        if (req.active() != null) p.setActive(req.active());
        return products.save(p);
    }

    @Transactional
    public void delete(Long id) {
        products.delete(get(id));
    }

    @Transactional
    public Product adjustStock(Long id, int delta) {
        Product p = get(id);
        int updated = p.getQuantityAvailable() + delta;
        if (updated < 0) {
            throw new BusinessRuleException("Stock cannot go negative");
        }
        p.setQuantityAvailable(updated);
        return products.save(p);
    }

    @Transactional
    public ReserveResponse reserve(ReserveRequest req) {
        Product p = products.findBySku(req.sku()).orElse(null);
        if (p == null || !Boolean.TRUE.equals(p.getActive())
                || p.getQuantityAvailable() < req.quantity()) {
            Integer available = (p == null) ? null : p.getQuantityAvailable();
            return new ReserveResponse(req.sku(), ReservationStatus.FAILED, available);
        }
        p.setQuantityAvailable(p.getQuantityAvailable() - req.quantity());
        products.save(p);
        return new ReserveResponse(req.sku(), ReservationStatus.HELD, p.getQuantityAvailable());
    }

    @Transactional(readOnly = true)
    public ReserveResponse confirm(ReserveRequest req) {
        Product p = getBySku(req.sku());
        return new ReserveResponse(req.sku(), ReservationStatus.CONFIRMED, p.getQuantityAvailable());
    }

    @Transactional
    public ReserveResponse release(ReserveRequest req) {
        Product p = getBySku(req.sku());
        p.setQuantityAvailable(p.getQuantityAvailable() + req.quantity());
        products.save(p);
        return new ReserveResponse(req.sku(), ReservationStatus.RELEASED, p.getQuantityAvailable());
    }
}
