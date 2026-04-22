package com.example.tracking_order.modules.order.service;

import com.example.tracking_order.modules.order.dto.CreateTrackingLogRequest;
import com.example.tracking_order.modules.order.dto.TrackingLogDto;

import java.util.List;

public interface TrackingService {
    TrackingLogDto addTrackingLog(Long orderId, CreateTrackingLogRequest request);
    List<TrackingLogDto> getTrackingLogs(Long orderId);
}
