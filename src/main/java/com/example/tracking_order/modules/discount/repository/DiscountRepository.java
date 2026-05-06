package com.example.tracking_order.modules.discount.repository;

import com.example.tracking_order.modules.discount.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Discount d WHERE d.code = :code AND d.isActive = true")
    Optional<Discount> findByCodeAndIsActiveTrueWithLock(String code);

    Optional<Discount> findByCodeAndIsActiveTrue(String code);
}
