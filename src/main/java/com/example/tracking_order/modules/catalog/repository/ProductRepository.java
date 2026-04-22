package com.example.tracking_order.modules.catalog.repository;

import com.example.tracking_order.modules.catalog.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    
    boolean existsBySku(String sku);

    // Dùng EntityGraph để fetch luôn seller và category để tránh N+1
    @EntityGraph(attributePaths = {"seller", "category"})
    Optional<Product> findById(Long id);
    
    @EntityGraph(attributePaths = {"seller", "category"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(Long id);
}
