package com.ejada.shop.dto.response;

import com.ejada.shop.entity.PaymentMethod;

public record PaymentMethodResponse(
        Long id,
        String code,
        String label,
        Boolean walletBacked,
        Boolean active
) {
    public static PaymentMethodResponse from(PaymentMethod m) {
        return new PaymentMethodResponse(m.getId(), m.getCode(), m.getLabel(), m.getWalletBacked(), m.getActive());
    }
}
