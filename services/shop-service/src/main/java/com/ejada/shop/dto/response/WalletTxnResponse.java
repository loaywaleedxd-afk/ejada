package com.ejada.shop.dto.response;

import java.math.BigDecimal;

public record WalletTxnResponse(
        String reference,
        String status,
        BigDecimal balanceAfter
) {
}
