package com.example.tracking_order.modules.order.repository;

import com.example.tracking_order.modules.order.entity.TrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingLogRepository extends JpaRepository<TrackingLog, Long> {
    List<TrackingLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
