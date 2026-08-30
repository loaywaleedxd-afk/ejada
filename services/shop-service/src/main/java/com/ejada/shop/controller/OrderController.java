package com.ejada.shop.controller;

import com.ejada.shop.dto.request.CheckoutRequest;
import com.ejada.shop.dto.response.OrderResponse;
import com.ejada.shop.entity.Order;
import com.ejada.shop.security.AccessGuard;
import com.ejada.shop.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AccessGuard accessGuard;

    @PostMapping("/checkout/{userId}")
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long userId,
                                                  @RequestBody(required = false) CheckoutRequest req,
                                                  HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        String method = (req == null || req.paymentMethodCode() == null || req.paymentMethodCode().isBlank())
                ? "WALLET" : req.paymentMethodCode();
        String discount = req == null ? null : req.discountCode();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(userId, method, discount));
    }

    @GetMapping("/user/{userId}")
    public List<Order> byUser(@PathVariable Long userId, HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return orderService.byUser(userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse detail(@PathVariable Long orderId, HttpServletRequest request) {
        OrderResponse order = orderService.detail(orderId);
        accessGuard.requireSelfOrAdmin(request, order.userId());
        return order;
    }
}
