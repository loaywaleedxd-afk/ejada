package com.ejada.shop.config;

import com.ejada.shop.entity.PaymentMethod;
import com.ejada.shop.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMethodSeeder implements ApplicationRunner {

    private final PaymentMethodRepository methods;

    @Override
    public void run(ApplicationArguments args) {
        if (!methods.existsByCodeIgnoreCase("WALLET")) {
            methods.save(PaymentMethod.builder()
                    .code("WALLET")
                    .label("Wallet balance")
                    .walletBacked(true)
                    .active(true)
                    .build());
        }
    }
}
