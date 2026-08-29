package com.ejada.inventory.dto.request;

import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull Integer delta
) {
}
