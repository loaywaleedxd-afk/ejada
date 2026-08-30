package com.ejada.shop.service;

import com.ejada.shop.client.InventoryClient;
import com.ejada.shop.client.WalletClient;
import com.ejada.shop.domain.CartStatus;
import com.ejada.shop.domain.OrderStatus;
import com.ejada.shop.domain.PaymentStatus;
import com.ejada.shop.dto.request.*;
import com.ejada.shop.dto.response.*;
import com.ejada.shop.entity.*;
import com.ejada.shop.exception.BusinessRuleException;
import com.ejada.shop.exception.ResourceNotFoundException;
import com.ejada.shop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String RESERVATION_HELD = "HELD";
    private static final String WALLET_COMPLETED = "COMPLETED";

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final PaymentMethodRepository paymentMethods;
    private final DiscountCodeRepository discounts;
    private final WalletClient walletClient;
    private final InventoryClient inventoryClient;

    private record Line(Long productId, String sku, String name, int quantity, BigDecimal unitPrice) {
        BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public OrderResponse checkout(Long userId, String paymentMethodCode, String discountCode) {
        Cart cart = carts.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart for user " + userId));

        List<Line> lines = resolveLines(cart.getId());
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Cart is empty");
        }

        PaymentMethod method = paymentMethods.findByCodeIgnoreCaseAndActiveTrue(paymentMethodCode.trim())
                .orElseThrow(() -> new BusinessRuleException(
                        "Payment method '" + paymentMethodCode + "' is not available"));

        BigDecimal subtotal = lines.stream().map(Line::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        int discountPercent = 0;
        String appliedCode = null;
        if (discountCode != null && !discountCode.isBlank()) {
            DiscountCode dc = discounts.findByCodeIgnoreCaseAndActiveTrue(discountCode.trim())
                    .orElseThrow(() -> new BusinessRuleException(
                            "Invalid or inactive discount code: " + discountCode));
            discountPercent = dc.getPercentage();
            appliedCode = dc.getCode();
        }

        BigDecimal discountAmount = subtotal
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discountAmount);

        Order order = persistOrder(userId, subtotal, total, appliedCode, discountPercent, method.getCode(), lines);
        String orderRef = order.getOrderNumber();

        List<Line> reserved = new ArrayList<>();
        for (Line l : lines) {
            ReservationResponse r = inventoryClient.reserve(new ReserveStockRequest(l.sku(), l.quantity(), orderRef));
            if (!RESERVATION_HELD.equals(r.status())) {
                releaseAll(reserved, orderRef);
                cancel(order);
                throw new BusinessRuleException("Insufficient stock for SKU " + l.sku());
            }
            reserved.add(l);
        }

        if (method.getWalletBacked()) {
            WalletTxnResponse debit = walletClient.debit(new DebitRequest(userId, total, orderRef, orderRef));
            if (!WALLET_COMPLETED.equals(debit.status())) {
                releaseAll(reserved, orderRef);
                recordPayment(order, total, PaymentStatus.FAILED, method.getCode());
                cancel(order);
                throw new BusinessRuleException("Payment failed (insufficient funds or wallet unavailable)");
            }
        }

        for (Line l : reserved) {
            inventoryClient.confirm(new ReserveStockRequest(l.sku(), l.quantity(), orderRef));
        }
        recordPayment(order, total, PaymentStatus.SUCCESS, method.getCode());
        order.setStatus(OrderStatus.PAID);
        orders.save(order);

        cart.setStatus(CartStatus.CONVERTED);
        carts.save(cart);

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<Order> byUser(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public OrderResponse detail(Long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
        return toResponse(order);
    }

    private List<Line> resolveLines(Long cartId) {
        List<Line> lines = new ArrayList<>();
        for (CartItem ci : cartItems.findByCartId(cartId)) {
            Product p = products.findById(ci.getProductId())
                    .orElseThrow(() -> new BusinessRuleException("Product " + ci.getProductId() + " no longer exists"));
            lines.add(new Line(p.getId(), p.getSku(), p.getName(), ci.getQuantity(), ci.getUnitPrice()));
        }
        return lines;
    }

    private Order persistOrder(Long userId, BigDecimal subtotal, BigDecimal total, String discountCode,
                               int discountPercent, String paymentMethod, List<Line> lines) {
        Order order = orders.save(Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .status(OrderStatus.CREATED)
                .subtotalAmount(subtotal)
                .discountPercent(discountPercent)
                .discountCode(discountCode)
                .paymentMethod(paymentMethod)
                .totalAmount(total)
                .build());
        for (Line l : lines) {
            orderItems.save(OrderItem.builder()
                    .orderId(order.getId())
                    .productId(l.productId())
                    .productName(l.name())
                    .quantity(l.quantity())
                    .unitPrice(l.unitPrice())
                    .build());
        }
        return order;
    }

    private void releaseAll(List<Line> reserved, String orderRef) {
        for (Line l : reserved) {
            inventoryClient.release(new ReserveStockRequest(l.sku(), l.quantity(), orderRef));
        }
    }

    private void cancel(Order order) {
        order.setStatus(OrderStatus.CANCELLED);
        orders.save(order);
    }

    private void recordPayment(Order order, BigDecimal amount, PaymentStatus status, String method) {
        payments.save(Payment.builder()
                .orderId(order.getId())
                .amount(amount)
                .method(method)
                .status(status)
                .build());
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.Item> items = orderItems.findByOrderId(order.getId()).stream()
                .map(oi -> new OrderResponse.Item(oi.getProductId(), oi.getProductName(),
                        oi.getQuantity(), oi.getUnitPrice(),
                        oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity()))))
                .toList();
        OrderResponse.Payment pay = payments.findByOrderId(order.getId()).stream().findFirst()
                .map(p -> new OrderResponse.Payment(p.getId(), p.getAmount(), p.getMethod(), p.getStatus()))
                .orElse(null);
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getUserId(),
                order.getStatus(), order.getSubtotalAmount(), order.getDiscountPercent(),
                order.getDiscountCode(), order.getTotalAmount(), order.getPaymentMethod(),
                order.getCreatedAt(), items, pay);
    }
}
