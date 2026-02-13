package com.notifyhub.service;

import com.notifyhub.dto.NotificationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Global notification sink for SSE streaming.
 * Pushes notifications to connected users via Server-Sent Events.
 */
@Slf4j
@Service
public class NotificationSinkService {

    private final Sinks.Many<NotificationDto> sink = Sinks.many().multicast().onBackpressureBuffer(1000);

    public void push(NotificationDto notification) {
        Sinks.EmitResult result = sink.tryEmitNext(notification);
        if (result.isFailure()) {
            log.warn("Failed to emit notification to sink: {}", result);
        }
    }

    /**
     * Stream notifications for a specific user.
     * Filters the global stream to only include notifications for the given userId.
     */
    public Flux<NotificationDto> streamForUser(Long userId) {
        return sink.asFlux()
                .filter(n -> n.getUserId().equals(userId));
    }
}
