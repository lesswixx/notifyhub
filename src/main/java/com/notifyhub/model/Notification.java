package com.notifyhub.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("notifications")
public class Notification {
    @Id
    private Long id;
    private Long userId;
    private UUID eventId;
    private String channel;
    @Builder.Default
    private String status = "CREATED";
    @Builder.Default
    private Integer attempts = 0;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
