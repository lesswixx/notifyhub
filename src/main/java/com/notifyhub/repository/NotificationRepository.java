package com.notifyhub.repository;

import com.notifyhub.model.Notification;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface NotificationRepository extends ReactiveCrudRepository<Notification, Long> {
    Flux<Notification> findByUserId(Long userId);
    Flux<Notification> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND created_at >= :from AND created_at <= :to ORDER BY created_at DESC")
    Flux<Notification> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Notification> findByUserIdPaginated(Long userId, int limit, long offset);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND channel = :channel AND created_at >= :since")
    Mono<Long> countByUserIdAndChannelSince(Long userId, String channel, LocalDateTime since);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Notification> findByUserIdAndStatusPaginated(Long userId, String status, int limit, long offset);
}
