package com.ejada.wallet.dto.response;

import com.ejada.wallet.domain.TransactionType;
import com.ejada.wallet.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String reference,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType(), t.getAmount(),
                t.getBalanceAfter(), t.getReference(), t.getCreatedAt());
    }
}
