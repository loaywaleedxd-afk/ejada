package com.ejada.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discount_codes",
        uniqueConstraints = @UniqueConstraint(name = "uk_discount_codes_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private Integer percentage;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
