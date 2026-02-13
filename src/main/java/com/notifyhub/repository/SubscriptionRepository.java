package com.notifyhub.repository;

import com.notifyhub.model.Subscription;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SubscriptionRepository extends ReactiveCrudRepository<Subscription, Long> {
    Flux<Subscription> findByUserId(Long userId);
    Flux<Subscription> findByEnabled(Boolean enabled);
    Flux<Subscription> findBySourceTypeAndEnabled(String sourceType, Boolean enabled);
}
