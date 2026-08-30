package com.ejada.shop.service;

import com.ejada.shop.dto.request.PaymentMethodRequest;
import com.ejada.shop.entity.PaymentMethod;
import com.ejada.shop.exception.ConflictException;
import com.ejada.shop.exception.ResourceNotFoundException;
import com.ejada.shop.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository methods;

    @Transactional(readOnly = true)
    public List<PaymentMethod> findAll() {
        return methods.findAll();
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> findActive() {
        return methods.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<PaymentMethod> findActiveByCode(String code) {
        return code == null ? Optional.empty() : methods.findByCodeIgnoreCaseAndActiveTrue(code.trim());
    }

    @Transactional
    public PaymentMethod create(PaymentMethodRequest req) {
        String code = req.code().trim().toUpperCase();
        if (methods.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Payment method already exists: " + code);
        }
        return methods.save(PaymentMethod.builder()
                .code(code)
                .label(req.label())
                .walletBacked(req.walletBacked() != null && req.walletBacked())
                .active(req.active() == null || req.active())
                .build());
    }

    @Transactional
    public void delete(Long id) {
        PaymentMethod m = methods.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method " + id + " not found"));
        methods.delete(m);
    }
}
