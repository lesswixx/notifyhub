package com.notifyhub.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("rules")
public class Rule {
    @Id
    private Long id;
    private Long subscriptionId;
    private String keywordFilter;
    @Builder.Default
    private Integer dedupWindowMinutes = 0;
    @Builder.Default
    private Integer rateLimitPerHour = 0;
    @Builder.Default
    private String priority = "MEDIUM";
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private LocalDateTime createdAt;
}
