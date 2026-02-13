package com.notifyhub.service;

import com.notifyhub.dto.RuleDto;
import com.notifyhub.model.Rule;
import com.notifyhub.repository.RuleRepository;
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
public class RuleService {

    private final RuleRepository ruleRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Flux<RuleDto> findBySubscriptionId(Long subscriptionId) {
        return ruleRepository.findBySubscriptionId(subscriptionId)
                .map(this::toDto);
    }

    public Mono<RuleDto> create(Long userId, RuleDto dto) {
        return subscriptionRepository.findById(dto.getSubscriptionId())
                .filter(s -> s.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Subscription not found or access denied")))
                .flatMap(sub -> {
                    Rule rule = Rule.builder()
                            .subscriptionId(dto.getSubscriptionId())
                            .keywordFilter(dto.getKeywordFilter())
                            .dedupWindowMinutes(dto.getDedupWindowMinutes() != null ? dto.getDedupWindowMinutes() : 0)
                            .rateLimitPerHour(dto.getRateLimitPerHour() != null ? dto.getRateLimitPerHour() : 0)
                            .priority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM")
                            .quietHoursStart(dto.getQuietHoursStart())
                            .quietHoursEnd(dto.getQuietHoursEnd())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return ruleRepository.save(rule);
                })
                .doOnNext(r -> log.info("Rule created: id={}, subId={}", r.getId(), r.getSubscriptionId()))
                .map(this::toDto);
    }

    public Mono<RuleDto> update(Long userId, Long id, RuleDto dto) {
        return ruleRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Rule not found")))
                .flatMap(existing -> subscriptionRepository.findById(existing.getSubscriptionId())
                        .filter(s -> s.getUserId().equals(userId))
                        .switchIfEmpty(Mono.error(new RuntimeException("Access denied")))
                        .thenReturn(existing))
                .flatMap(existing -> {
                    if (dto.getKeywordFilter() != null) existing.setKeywordFilter(dto.getKeywordFilter());
                    if (dto.getDedupWindowMinutes() != null) existing.setDedupWindowMinutes(dto.getDedupWindowMinutes());
                    if (dto.getRateLimitPerHour() != null) existing.setRateLimitPerHour(dto.getRateLimitPerHour());
                    if (dto.getPriority() != null) existing.setPriority(dto.getPriority());
                    if (dto.getQuietHoursStart() != null) existing.setQuietHoursStart(dto.getQuietHoursStart());
                    if (dto.getQuietHoursEnd() != null) existing.setQuietHoursEnd(dto.getQuietHoursEnd());
                    return ruleRepository.save(existing);
                })
                .map(this::toDto);
    }

    public Mono<Void> delete(Long userId, Long id) {
        return ruleRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Rule not found")))
                .flatMap(existing -> subscriptionRepository.findById(existing.getSubscriptionId())
                        .filter(s -> s.getUserId().equals(userId))
                        .switchIfEmpty(Mono.error(new RuntimeException("Access denied")))
                        .thenReturn(existing))
                .flatMap(r -> ruleRepository.deleteById(r.getId()));
    }

    private RuleDto toDto(Rule r) {
        return RuleDto.builder()
                .id(r.getId())
                .subscriptionId(r.getSubscriptionId())
                .keywordFilter(r.getKeywordFilter())
                .dedupWindowMinutes(r.getDedupWindowMinutes())
                .rateLimitPerHour(r.getRateLimitPerHour())
                .priority(r.getPriority())
                .quietHoursStart(r.getQuietHoursStart())
                .quietHoursEnd(r.getQuietHoursEnd())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
