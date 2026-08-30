package com.ejada.shop.repository;

import com.ejada.shop.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByCodeIgnoreCase(String code);

    Optional<PaymentMethod> findByCodeIgnoreCaseAndActiveTrue(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<PaymentMethod> findByActiveTrue();
}
