package com.ejada.wallet.dto.response;

import com.ejada.wallet.domain.WalletStatus;
import com.ejada.wallet.entity.Wallet;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        String currency,
        WalletStatus status
) {
    public static WalletResponse from(Wallet w) {
        return new WalletResponse(w.getId(), w.getUserId(), w.getBalance(),
                w.getCurrency(), w.getStatus());
    }
}
