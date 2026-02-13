package com.notifyhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleDto {
    private Long id;
    private Long subscriptionId;
    private String keywordFilter;
    private Integer dedupWindowMinutes;
    private Integer rateLimitPerHour;
    private String priority;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private LocalDateTime createdAt;
}
