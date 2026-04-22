package com.example.tracking_order.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrackingLogDto {
    private Long id;
    private String event_type;
    private String location;
    private String note;
    private UserRef logged_by;
    private LocalDateTime created_at;

    @Data
    @Builder
    public static class UserRef {
        private Long id;
        private String name;
    }
}
