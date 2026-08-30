package com.ejada.shop.dto.response;

import com.ejada.shop.entity.DiscountCode;

public record DiscountResponse(
        Long id,
        String code,
        Integer percentage,
        Boolean active
) {
    public static DiscountResponse from(DiscountCode d) {
        return new DiscountResponse(d.getId(), d.getCode(), d.getPercentage(), d.getActive());
    }
}
