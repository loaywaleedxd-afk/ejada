package com.ejada.shop.dto.request;

import java.math.BigDecimal;

public record DebitRequest(
        Long userId,
        BigDecimal amount,
        String reference,
        String orderRef
) {
}
