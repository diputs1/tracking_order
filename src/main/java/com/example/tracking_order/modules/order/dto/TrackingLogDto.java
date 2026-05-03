package com.example.tracking_order.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrackingLogDto {
    private Long id;
    @JsonProperty("event_type")
    private String eventType;
    private String location;
    private String note;
    @JsonProperty("logged_by")
    private UserRef loggedBy;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class UserRef {
        private Long id;
        private String name;
    }
}
