package com.ejada.shop.service;

import com.ejada.shop.domain.CartStatus;
import com.ejada.shop.dto.response.CartResponse;
import com.ejada.shop.entity.Cart;
import com.ejada.shop.entity.CartItem;
import com.ejada.shop.entity.Product;
import com.ejada.shop.exception.BusinessRuleException;
import com.ejada.shop.exception.ResourceNotFoundException;
import com.ejada.shop.repository.CartItemRepository;
import com.ejada.shop.repository.CartRepository;
import com.ejada.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;

    @Transactional
    public Cart getOrCreateActiveCart(Long userId) {
        return carts.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> carts.save(Cart.builder().userId(userId).status(CartStatus.ACTIVE).build()));
    }

    @Transactional
    public CartResponse addItem(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateActiveCart(userId);
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BusinessRuleException("Product is not available");
        }
        CartItem item = cartItems.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(() -> CartItem.builder()
                        .cartId(cart.getId())
                        .productId(productId)
                        .quantity(0)
                        .unitPrice(product.getPrice())
                        .build());
        item.setQuantity(item.getQuantity() + quantity);
        item.setUnitPrice(product.getPrice());
        cartItems.save(item);
        return view(userId);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateActiveCart(userId);
        cartItems.findByCartIdAndProductId(cart.getId(), productId).ifPresent(cartItems::delete);
        return view(userId);
    }

    @Transactional
    public void clear(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        cartItems.deleteByCartId(cart.getId());
    }

    @Transactional(readOnly = true)
    public CartResponse view(Long userId) {
        Cart cart = carts.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart for user " + userId));
        return toResponse(cart);
    }

    CartResponse toResponse(Cart cart) {
        List<CartResponse.Item> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cartItems.findByCartId(cart.getId())) {
            String name = products.findById(ci.getProductId()).map(Product::getName).orElse("(removed)");
            BigDecimal line = ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            total = total.add(line);
            rows.add(new CartResponse.Item(ci.getProductId(), name, ci.getQuantity(), ci.getUnitPrice(), line));
        }
        return new CartResponse(cart.getId(), cart.getUserId(), cart.getStatus(), rows, total);
    }
}
