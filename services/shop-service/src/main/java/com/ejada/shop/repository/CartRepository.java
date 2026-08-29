package com.ejada.shop.repository;

import com.ejada.shop.domain.CartStatus;
import com.ejada.shop.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
}
