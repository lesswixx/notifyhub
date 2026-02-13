package com.notifyhub.controller;

import com.notifyhub.dto.RuleDto;
import com.notifyhub.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public Flux<RuleDto> list(@RequestParam Long subscriptionId) {
        return ruleService.findBySubscriptionId(subscriptionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RuleDto> create(@AuthenticationPrincipal Long userId,
                                 @RequestBody RuleDto dto) {
        return ruleService.create(userId, dto);
    }

    @PutMapping("/{id}")
    public Mono<RuleDto> update(@AuthenticationPrincipal Long userId,
                                 @PathVariable Long id,
                                 @RequestBody RuleDto dto) {
        return ruleService.update(userId, id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@AuthenticationPrincipal Long userId,
                              @PathVariable Long id) {
        return ruleService.delete(userId, id);
    }
}
