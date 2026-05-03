package com.example.tracking_order.modules.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.tracking_order.modules.order.enums.TrackingEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTrackingLogRequest {
    @NotNull(message = "Loại sự kiện không được để trống")
    @JsonProperty("event_type")
    private TrackingEventType eventType;

    @NotBlank(message = "Vị trí không được để trống")
    private String location;

    private String note;
}
