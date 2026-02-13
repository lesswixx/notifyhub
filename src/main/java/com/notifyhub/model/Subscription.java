package com.notifyhub.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("subscriptions")
public class Subscription {
    @Id
    private Long id;
    private Long userId;
    private String sourceType;
    private String params;
    @Builder.Default
    private Boolean enabled = true;
    private LocalDateTime createdAt;
}
