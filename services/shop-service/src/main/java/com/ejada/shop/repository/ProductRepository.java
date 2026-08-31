package com.ejada.shop.repository;

import com.ejada.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByActiveTrue();

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Query("select distinct p.category from Product p where p.category is not null and p.category <> '' order by p.category")
    List<String> findDistinctCategories();
}
