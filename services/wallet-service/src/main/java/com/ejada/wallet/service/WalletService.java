package com.ejada.wallet.service;

import com.ejada.wallet.domain.TransactionResult;
import com.ejada.wallet.domain.TransactionType;
import com.ejada.wallet.dto.request.DebitRequest;
import com.ejada.wallet.dto.response.WalletTxnResponse;
import com.ejada.wallet.entity.Transaction;
import com.ejada.wallet.entity.Wallet;
import com.ejada.wallet.exception.BusinessRuleException;
import com.ejada.wallet.repository.TransactionRepository;
import com.ejada.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository wallets;
    private final TransactionRepository transactions;

    @Transactional
    public Wallet createWallet(Long userId) {
        return wallets.findByUserId(userId)
                .orElseGet(() -> wallets.save(Wallet.builder().userId(userId).build()));
    }

    @Transactional
    public Wallet getByUserId(Long userId) {
        return createWallet(userId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> history(Long userId) {
        return wallets.findByUserId(userId)
                .map(w -> transactions.findByWalletIdOrderByCreatedAtDesc(w.getId()))
                .orElseGet(List::of);
    }

    @Transactional
    public Wallet deposit(Long userId, BigDecimal amount, String reference) {
        Wallet w = getByUserId(userId);
        w.setBalance(w.getBalance().add(amount));
        wallets.save(w);
        record(w, ref(reference), TransactionType.DEPOSIT, amount);
        return w;
    }

    @Transactional
    public Wallet withdraw(Long userId, BigDecimal amount, String reference) {
        Wallet w = getByUserId(userId);
        if (hasInsufficientFunds(w, amount)) {
            throw new BusinessRuleException("Insufficient funds");
        }
        w.setBalance(w.getBalance().subtract(amount));
        wallets.save(w);
        record(w, ref(reference), TransactionType.WITHDRAWAL, amount);
        return w;
    }

    @Transactional
    public WalletTxnResponse debit(DebitRequest req) {
        var existing = transactions.findByReference(req.reference());
        if (existing.isPresent()) {
            return completed(req.reference(), existing.get().getBalanceAfter());
        }
        Wallet w = getByUserId(req.userId());
        if (hasInsufficientFunds(w, req.amount())) {
            return new WalletTxnResponse(req.reference(), TransactionResult.FAILED, w.getBalance());
        }
        w.setBalance(w.getBalance().subtract(req.amount()));
        wallets.save(w);
        record(w, req.reference(), TransactionType.PAYMENT, req.amount());
        return completed(req.reference(), w.getBalance());
    }

    @Transactional
    public WalletTxnResponse refund(DebitRequest req) {
        var existing = transactions.findByReference(req.reference());
        if (existing.isPresent()) {
            return completed(req.reference(), existing.get().getBalanceAfter());
        }
        Wallet w = getByUserId(req.userId());
        w.setBalance(w.getBalance().add(req.amount()));
        wallets.save(w);
        record(w, req.reference(), TransactionType.REFUND, req.amount());
        return completed(req.reference(), w.getBalance());
    }

    private boolean hasInsufficientFunds(Wallet w, BigDecimal amount) {
        return w.getBalance().compareTo(amount) < 0;
    }

    private WalletTxnResponse completed(String reference, BigDecimal balanceAfter) {
        return new WalletTxnResponse(reference, TransactionResult.COMPLETED, balanceAfter);
    }

    private void record(Wallet w, String reference, TransactionType type, BigDecimal amount) {
        transactions.save(Transaction.builder()
                .walletId(w.getId())
                .reference(reference)
                .type(type)
                .amount(amount)
                .balanceAfter(w.getBalance())
                .build());
    }

    private String ref(String reference) {
        return (reference == null || reference.isBlank()) ? UUID.randomUUID().toString() : reference;
    }
}
