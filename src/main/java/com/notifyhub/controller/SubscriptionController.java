package com.notifyhub.controller;

import com.notifyhub.dto.SubscriptionDto;
import com.notifyhub.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public Flux<SubscriptionDto> list(@AuthenticationPrincipal Long userId) {
        return subscriptionService.findByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SubscriptionDto> create(@AuthenticationPrincipal Long userId,
                                         @RequestBody SubscriptionDto dto) {
        return subscriptionService.create(userId, dto);
    }

    @PutMapping("/{id}")
    public Mono<SubscriptionDto> update(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long id,
                                         @RequestBody SubscriptionDto dto) {
        return subscriptionService.update(userId, id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@AuthenticationPrincipal Long userId,
                              @PathVariable Long id) {
        return subscriptionService.delete(userId, id);
    }
}
