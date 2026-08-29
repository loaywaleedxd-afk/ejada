package com.ejada.shop.controller;

import com.ejada.shop.dto.request.AddToCartRequest;
import com.ejada.shop.dto.response.CartResponse;
import com.ejada.shop.security.AccessGuard;
import com.ejada.shop.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final AccessGuard accessGuard;

    @GetMapping("/{userId}")
    public CartResponse view(@PathVariable Long userId, HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return cartService.view(userId);
    }

    @PostMapping("/{userId}/items")
    public CartResponse addItem(@PathVariable Long userId, @Valid @RequestBody AddToCartRequest req,
                                HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return cartService.addItem(userId, req.productId(), req.quantity());
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public CartResponse removeItem(@PathVariable Long userId, @PathVariable Long productId,
                                   HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return cartService.removeItem(userId, productId);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clear(@PathVariable Long userId, HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        cartService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
