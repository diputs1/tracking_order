package com.example.tracking_order.modules.shipping.repository;

import com.example.tracking_order.modules.shipping.entity.ShippingCarrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShippingCarrierRepository extends JpaRepository<ShippingCarrier, Long> {
    List<ShippingCarrier> findByIsActiveTrue();
}
