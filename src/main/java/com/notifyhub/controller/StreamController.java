package com.notifyhub.controller;

import com.notifyhub.dto.NotificationDto;
import com.notifyhub.service.NotificationSinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {

    private final NotificationSinkService sinkService;

    /**
     * SSE endpoint for real-time notifications.
     * Sends heartbeat every 30 seconds to keep connection alive.
     */
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationDto>> stream(@AuthenticationPrincipal Long userId) {
        log.info("SSE connection opened for user {}", userId);

        Flux<ServerSentEvent<NotificationDto>> notifications = sinkService.streamForUser(userId)
                .map(dto -> ServerSentEvent.<NotificationDto>builder()
                        .event("notification")
                        .data(dto)
                        .build());

        Flux<ServerSentEvent<NotificationDto>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<NotificationDto>builder()
                        .event("heartbeat")
                        .comment("keepalive")
                        .build());

        return Flux.merge(notifications, heartbeat)
                .doOnCancel(() -> log.info("SSE connection closed for user {}", userId));
    }
}
