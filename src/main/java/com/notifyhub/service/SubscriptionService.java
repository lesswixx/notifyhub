package com.notifyhub.service;

import com.notifyhub.dto.SubscriptionDto;
import com.notifyhub.model.Subscription;
import com.notifyhub.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public Flux<SubscriptionDto> findByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(this::toDto);
    }

    public Mono<SubscriptionDto> create(Long userId, SubscriptionDto dto) {
        Subscription subscription = Subscription.builder()
                .userId(userId)
                .sourceType(dto.getSourceType())
                .params(dto.getParams())
                .enabled(dto.getEnabled() != null ? dto.getEnabled() : true)
                .createdAt(LocalDateTime.now())
                .build();

        return subscriptionRepository.save(subscription)
                .doOnNext(s -> log.info("Subscription created: id={}, userId={}, source={}", s.getId(), userId, s.getSourceType()))
                .map(this::toDto);
    }

    public Mono<SubscriptionDto> update(Long userId, Long id, SubscriptionDto dto) {
        return subscriptionRepository.findById(id)
                .filter(s -> s.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Subscription not found")))
                .flatMap(existing -> {
                    if (dto.getSourceType() != null) existing.setSourceType(dto.getSourceType());
                    if (dto.getParams() != null) existing.setParams(dto.getParams());
                    if (dto.getEnabled() != null) existing.setEnabled(dto.getEnabled());
                    return subscriptionRepository.save(existing);
                })
                .map(this::toDto);
    }

    public Mono<Void> delete(Long userId, Long id) {
        return subscriptionRepository.findById(id)
                .filter(s -> s.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Subscription not found")))
                .flatMap(s -> subscriptionRepository.deleteById(s.getId()));
    }

    public Flux<Subscription> findAllEnabled() {
        return subscriptionRepository.findByEnabled(true);
    }

    private SubscriptionDto toDto(Subscription s) {
        return SubscriptionDto.builder()
                .id(s.getId())
                .sourceType(s.getSourceType())
                .params(s.getParams())
                .enabled(s.getEnabled())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
