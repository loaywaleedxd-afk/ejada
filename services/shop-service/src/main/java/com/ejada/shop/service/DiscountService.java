package com.ejada.shop.service;

import com.ejada.shop.dto.request.DiscountRequest;
import com.ejada.shop.entity.DiscountCode;
import com.ejada.shop.exception.ConflictException;
import com.ejada.shop.exception.ResourceNotFoundException;
import com.ejada.shop.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountCodeRepository discounts;

    @Transactional(readOnly = true)
    public List<DiscountCode> findAll() {
        return discounts.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<DiscountCode> findActiveByCode(String code) {
        return code == null ? Optional.empty() : discounts.findByCodeIgnoreCaseAndActiveTrue(code.trim());
    }

    @Transactional
    public DiscountCode create(DiscountRequest req) {
        String code = req.code().trim().toUpperCase();
        if (discounts.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Discount code already exists: " + code);
        }
        return discounts.save(DiscountCode.builder()
                .code(code)
                .percentage(req.percentage())
                .active(req.active() == null || req.active())
                .build());
    }

    @Transactional
    public void delete(Long id) {
        DiscountCode d = discounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code " + id + " not found"));
        discounts.delete(d);
    }
}
