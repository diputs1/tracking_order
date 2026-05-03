package com.example.tracking_order.modules.catalog.repository;

import com.example.tracking_order.modules.catalog.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    @EntityGraph(attributePaths = {"seller", "category", "inventory"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Query("SELECT p.id as id, p.name as name, p.sku as sku, p.slug as slug, " +
           "p.basePrice as basePrice, p.salePrice as salePrice, p.status as status, " +
           "c.id as categoryId, c.name as categoryName, " +
           "s.id as sellerId, s.fullName as sellerFullName, " +
           "i.quantityInStock as quantityInStock, i.quantityReserved as quantityReserved " +
           "FROM Product p " +
           "LEFT JOIN p.category c " +
           "LEFT JOIN p.seller s " +
           "LEFT JOIN p.inventory i")
    Page<com.example.tracking_order.modules.catalog.dto.ProductProjection> findAllProjected(Specification<Product> spec, Pageable pageable);
}
