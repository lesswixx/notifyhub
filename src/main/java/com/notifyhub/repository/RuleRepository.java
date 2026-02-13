package com.notifyhub.repository;

import com.notifyhub.model.Rule;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RuleRepository extends ReactiveCrudRepository<Rule, Long> {
    Flux<Rule> findBySubscriptionId(Long subscriptionId);
    Mono<Void> deleteBySubscriptionId(Long subscriptionId);
}
