package com.ejada.shop.client;

import com.ejada.shop.dto.request.DebitRequest;
import com.ejada.shop.dto.response.WalletTxnResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "walletClient", url = "${services.wallet.url}",
        path = "/api/wallet/wallets", fallback = WalletClientFallback.class)
public interface WalletClient {

    @PostMapping("/debit")
    WalletTxnResponse debit(@RequestBody DebitRequest request);

    @PostMapping("/refund")
    WalletTxnResponse refund(@RequestBody DebitRequest request);
}
