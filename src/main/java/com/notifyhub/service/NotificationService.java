package com.notifyhub.service;

import com.notifyhub.dto.NotificationDto;
import com.notifyhub.model.Event;
import com.notifyhub.model.Notification;
import com.notifyhub.repository.EventRepository;
import com.notifyhub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EventRepository eventRepository;
    private final NotificationSinkService sinkService;

    /**
     * Creates a notification, saves to DB, and pushes to SSE stream if channel is UI.
     */
    public Mono<Notification> createAndDeliver(Long userId, Event event, String channel) {
        Notification notification = Notification.builder()
                .userId(userId)
                .eventId(event.getId())
                .channel(channel)
                .status("CREATED")
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification)
                .doOnNext(saved -> {
                    if ("UI".equals(channel)) {
                        NotificationDto dto = toDto(saved, event);
                        sinkService.push(dto);
                    }
                    log.debug("Notification created: id={}, userId={}, channel={}", saved.getId(), userId, channel);
                });
    }

    public Mono<Notification> updateStatus(Long notificationId, String status, String error) {
        return notificationRepository.findById(notificationId)
                .flatMap(n -> {
                    n.setStatus(status);
                    n.setUpdatedAt(LocalDateTime.now());
                    n.setAttempts(n.getAttempts() + 1);
                    if (error != null) n.setLastError(error);
                    return notificationRepository.save(n);
                });
    }

    public Flux<NotificationDto> findByUserId(Long userId, int page, int size) {
        return notificationRepository.findByUserIdPaginated(userId, size, (long) page * size)
                .flatMap(this::enrichWithEvent);
    }

    public Flux<NotificationDto> findByUserIdAndStatus(Long userId, String status, int page, int size) {
        return notificationRepository.findByUserIdAndStatusPaginated(userId, status, size, (long) page * size)
                .flatMap(this::enrichWithEvent);
    }

    public Flux<NotificationDto> findByUserIdBetween(Long userId, LocalDateTime from, LocalDateTime to) {
        return notificationRepository.findByUserIdAndCreatedAtBetween(userId, from, to)
                .flatMap(this::enrichWithEvent);
    }

    private Mono<NotificationDto> enrichWithEvent(Notification n) {
        if (n.getEventId() == null) {
            return Mono.just(toDto(n, null));
        }
        return eventRepository.findById(n.getEventId())
                .map(event -> toDto(n, event))
                .defaultIfEmpty(toDto(n, null));
    }

    private NotificationDto toDto(Notification n, Event event) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .eventId(n.getEventId())
                .channel(n.getChannel())
                .status(n.getStatus())
                .attempts(n.getAttempts())
                .lastError(n.getLastError())
                .createdAt(n.getCreatedAt())
                .eventTitle(event != null ? event.getTitle() : null)
                .eventSourceType(event != null ? event.getSourceType() : null)
                .eventPriority(event != null ? event.getPriority() : null)
                .eventPayloadJson(event != null ? event.getPayloadJson() : null)
                .build();
    }
}
