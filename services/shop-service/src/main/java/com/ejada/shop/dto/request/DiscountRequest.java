package com.ejada.shop.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiscountRequest(
        @NotBlank @Size(max = 40) String code,
        @NotNull @Min(1) @Max(100) Integer percentage,
        Boolean active
) {
}
