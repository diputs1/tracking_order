package com.example.tracking_order.modules.order.service;

import com.example.tracking_order.modules.order.enums.OrderStatus;
import java.time.LocalDateTime;

public interface ExportService {
    byte[] exportOrdersToCsv(OrderStatus status, LocalDateTime fromDate, LocalDateTime toDate);
}
