package com.example.tracking_order.modules.order.service.impl;

import com.example.tracking_order.common.exception.AppException;
import com.example.tracking_order.common.exception.ErrorCode;
import com.example.tracking_order.modules.order.dto.CreateTrackingLogRequest;
import com.example.tracking_order.modules.order.dto.TrackingLogDto;
import com.example.tracking_order.modules.order.entity.Order;
import com.example.tracking_order.modules.order.entity.TrackingLog;
import com.example.tracking_order.modules.order.repository.OrderRepository;
import com.example.tracking_order.modules.order.repository.TrackingLogRepository;
import com.example.tracking_order.modules.order.service.TrackingService;
import com.example.tracking_order.modules.user.entity.User;
import com.example.tracking_order.modules.user.repository.UserRepository;
import com.example.tracking_order.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final TrackingLogRepository trackingLogRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TrackingLogDto addTrackingLog(Long orderId, CreateTrackingLogRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại"));

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User loggedBy = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Người dùng không hợp lệ"));

        TrackingLog trackingLog = TrackingLog.builder()
                .order(order)
                .eventType(request.getEvent_type())
                .location(request.getLocation())
                .note(request.getNote())
                .updatedBy(loggedBy)
                .toStatus(order.getStatus())
                .build();

        trackingLog = trackingLogRepository.save(trackingLog);

        return TrackingLogDto.builder()
                .id(trackingLog.getId())
                .event_type(trackingLog.getEventType().name())
                .location(trackingLog.getLocation())
                .note(trackingLog.getNote())
                .logged_by(TrackingLogDto.UserRef.builder()
                        .id(loggedBy.getId())
                        .name(loggedBy.getFullName())
                        .build())
                .created_at(trackingLog.getCreatedAt())
                .build();
    }

    @Override
    public List<TrackingLogDto> getTrackingLogs(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Đơn hàng không tồn tại");
        }

        List<TrackingLog> logs = trackingLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        return logs.stream().map(log -> TrackingLogDto.builder()
                .id(log.getId())
                .event_type(log.getEventType().name())
                .location(log.getLocation())
                .note(log.getNote())
                .logged_by(TrackingLogDto.UserRef.builder()
                        .id(log.getUpdatedBy().getId())
                        .name(log.getUpdatedBy().getFullName())
                        .build())
                .created_at(log.getCreatedAt())
                .build()).collect(Collectors.toList());
    }
}
