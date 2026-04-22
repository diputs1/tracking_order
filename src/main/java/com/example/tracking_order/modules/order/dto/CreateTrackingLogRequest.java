package com.example.tracking_order.modules.order.dto;

import com.example.tracking_order.modules.order.enums.TrackingEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTrackingLogRequest {
    @NotNull(message = "Loại sự kiện không được để trống")
    private TrackingEventType event_type;

    @NotBlank(message = "Vị trí không được để trống")
    private String location;

    private String note;
}
