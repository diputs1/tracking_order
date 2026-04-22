package com.example.tracking_order.modules.order.entity;

import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.entity.TrackingLog;

import com.example.tracking_order.modules.order.enums.OrderStatus;
import com.example.tracking_order.modules.order.enums.TrackingEventType;
import com.example.tracking_order.modules.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private OrderStatus toStatus;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    @Builder.Default
    private TrackingEventType eventType = TrackingEventType.SYSTEM;

    @Column(name = "proof_image_url")
    private String proofImageUrl;

    @Column(name = "event_time")
    private LocalDateTime eventTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
