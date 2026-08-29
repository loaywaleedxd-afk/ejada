package com.ejada.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "walletClient", url = "${services.wallet.url}",
        path = "/api/wallet/wallets", fallback = WalletClientFallback.class)
public interface WalletClient {

    @PostMapping("/{userId}")
    void createWallet(@PathVariable Long userId);
}
