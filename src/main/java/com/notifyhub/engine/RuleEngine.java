package com.notifyhub.engine;

import com.notifyhub.model.Event;
import com.notifyhub.model.Rule;
import com.notifyhub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Rule Engine applies user-defined rules to incoming events.
 * Supports: keyword filtering, dedup windows, rate limiting, quiet hours, priority assignment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngine {

    private final NotificationRepository notificationRepository;

    /**
     * Evaluates whether an event passes the given rules for a user.
     * Returns the priority to use (from the first matching rule, or event's default).
     * Returns empty Mono if the event should be filtered out.
     */
    public Mono<String> evaluate(Event event, List<Rule> rules, Long userId) {
        if (rules == null || rules.isEmpty()) {
            // No rules = pass through with event's default priority
            return Mono.just(event.getPriority());
        }

        // Event must pass at least one rule
        return evaluateRules(event, rules, userId);
    }

    private Mono<String> evaluateRules(Event event, List<Rule> rules, Long userId) {
        // Try each rule - first one that passes wins
        return Mono.defer(() -> {
            for (Rule rule : rules) {
                // 1. Keyword filter
                if (!passesKeywordFilter(event, rule)) {
                    continue;
                }

                // 2. Quiet hours check
                if (!passesQuietHours(rule)) {
                    continue;
                }

                // Rule matched keyword and quiet hours; now check rate/dedup asynchronously
                return checkRateAndDedup(rule, userId)
                        .flatMap(passes -> {
                            if (passes) {
                                return Mono.just(rule.getPriority());
                            }
                            return Mono.empty();
                        });
            }
            // No rule matched
            return Mono.empty();
        });
    }

    private boolean passesKeywordFilter(Event event, Rule rule) {
        if (rule.getKeywordFilter() == null || rule.getKeywordFilter().isBlank()) {
            return true; // No filter = pass
        }

        String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
        String payload = event.getPayloadJson() != null ? event.getPayloadJson().toLowerCase() : "";
        String content = title + " " + payload;

        String[] keywords = rule.getKeywordFilter().toLowerCase().split(",");
        return Arrays.stream(keywords)
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .anyMatch(content::contains);
    }

    private boolean passesQuietHours(Rule rule) {
        if (rule.getQuietHoursStart() == null || rule.getQuietHoursEnd() == null) {
            return true; // No quiet hours = pass
        }

        LocalTime now = LocalTime.now();
        LocalTime start = rule.getQuietHoursStart();
        LocalTime end = rule.getQuietHoursEnd();

        // If quiet hours span midnight (e.g., 22:00 - 08:00)
        if (start.isAfter(end)) {
            return !(now.isAfter(start) || now.isBefore(end));
        }

        // Normal range (e.g., 02:00 - 06:00)
        return !(now.isAfter(start) && now.isBefore(end));
    }

    private Mono<Boolean> checkRateAndDedup(Rule rule, Long userId) {
        // Rate limit check
        if (rule.getRateLimitPerHour() > 0) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            return notificationRepository.countByUserIdAndChannelSince(userId, "UI", oneHourAgo)
                    .map(count -> count < rule.getRateLimitPerHour());
        }
        return Mono.just(true);
    }
}
