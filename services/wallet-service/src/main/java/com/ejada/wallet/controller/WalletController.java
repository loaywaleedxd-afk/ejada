package com.ejada.wallet.controller;

import com.ejada.wallet.dto.request.*;
import com.ejada.wallet.dto.response.*;
import com.ejada.wallet.security.AccessGuard;
import com.ejada.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final AccessGuard accessGuard;

    @PostMapping("/{userId}")
    public WalletResponse createWallet(@PathVariable Long userId) {
        return WalletResponse.from(walletService.createWallet(userId));
    }

    @GetMapping("/{userId}")
    public WalletResponse get(@PathVariable Long userId, HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return WalletResponse.from(walletService.getByUserId(userId));
    }

    @PostMapping("/{userId}/deposit")
    public WalletResponse deposit(@PathVariable Long userId, @Valid @RequestBody AmountRequest req,
                                  HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return WalletResponse.from(walletService.deposit(userId, req.amount(), req.reference()));
    }

    @PostMapping("/{userId}/withdraw")
    public WalletResponse withdraw(@PathVariable Long userId, @Valid @RequestBody AmountRequest req,
                                   HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return WalletResponse.from(walletService.withdraw(userId, req.amount(), req.reference()));
    }

    @GetMapping("/{userId}/transactions")
    public List<TransactionResponse> transactions(@PathVariable Long userId, HttpServletRequest request) {
        accessGuard.requireSelfOrAdmin(request, userId);
        return walletService.history(userId).stream().map(TransactionResponse::from).toList();
    }

    @PostMapping("/debit")
    public WalletTxnResponse debit(@Valid @RequestBody DebitRequest req) {
        return walletService.debit(req);
    }

    @PostMapping("/refund")
    public WalletTxnResponse refund(@Valid @RequestBody DebitRequest req) {
        return walletService.refund(req);
    }
}
