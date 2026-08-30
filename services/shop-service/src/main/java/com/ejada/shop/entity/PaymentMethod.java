package com.ejada.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_methods",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_methods_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(name = "wallet_backed", nullable = false)
    @Builder.Default
    private Boolean walletBacked = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
