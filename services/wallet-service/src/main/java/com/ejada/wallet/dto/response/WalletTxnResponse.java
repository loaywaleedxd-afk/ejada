package com.ejada.wallet.dto.response;

import com.ejada.wallet.domain.TransactionResult;

import java.math.BigDecimal;

public record WalletTxnResponse(
        String reference,
        TransactionResult status,
        BigDecimal balanceAfter
) {
}
