package com.ejada.shop.client;

import com.ejada.shop.dto.request.DebitRequest;
import com.ejada.shop.dto.response.WalletTxnResponse;
import org.springframework.stereotype.Component;

@Component
public class WalletClientFallback implements WalletClient {

    @Override
    public WalletTxnResponse debit(DebitRequest request) {
        return new WalletTxnResponse(request.reference(), "FAILED", null);
    }

    @Override
    public WalletTxnResponse refund(DebitRequest request) {
        return new WalletTxnResponse(request.reference(), "FAILED", null);
    }
}
