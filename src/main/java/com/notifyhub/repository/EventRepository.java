package com.notifyhub.repository;

import com.notifyhub.model.Event;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface EventRepository extends ReactiveCrudRepository<Event, UUID> {
    Mono<Event> findByExternalId(String externalId);
    Mono<Boolean> existsByExternalId(String externalId);
    Flux<Event> findBySourceType(String sourceType);

    @Query("SELECT * FROM events WHERE created_at >= :from AND created_at <= :to ORDER BY created_at DESC")
    Flux<Event> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
