package com.notifyhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private Long userId;
    private UUID eventId;
    private String channel;
    private String status;
    private Integer attempts;
    private String lastError;
    private LocalDateTime createdAt;
    // Event details (denormalized for convenience)
    private String eventTitle;
    private String eventSourceType;
    private String eventPriority;
    private String eventPayloadJson;
}
