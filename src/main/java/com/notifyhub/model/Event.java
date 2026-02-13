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
@Table("events")
public class Event {
    @Id
    private UUID id;
    private String sourceType;
    private String externalId;
    private String title;
    private String payloadJson;
    @Builder.Default
    private String priority = "MEDIUM";
    private LocalDateTime createdAt;
}
