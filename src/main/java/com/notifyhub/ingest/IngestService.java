package com.notifyhub.ingest;

import com.notifyhub.engine.RuleEngine;
import com.notifyhub.model.Event;
import com.notifyhub.model.Notification;
import com.notifyhub.model.Rule;
import com.notifyhub.model.Subscription;
import com.notifyhub.delivery.DeliveryService;
import com.notifyhub.repository.EventRepository;
import com.notifyhub.repository.RuleRepository;
import com.notifyhub.repository.SubscriptionRepository;
import com.notifyhub.service.NotificationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main ingest pipeline.
 * Periodically polls all enabled subscriptions, fetches events from connectors,
 * deduplicates, applies rules, creates notifications, and triggers delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final RuleRepository ruleRepository;
    private final RuleEngine ruleEngine;
    private final NotificationService notificationService;
    private final DeliveryService deliveryService;
    private final List<SourceConnector> connectors;

    @Value("${app.ingest.poll-interval-seconds:60}")
    private int pollIntervalSeconds;

    private Map<String, SourceConnector> connectorMap;

    @PostConstruct
    public void start() {
        connectorMap = connectors.stream()
                .collect(Collectors.toMap(SourceConnector::getSourceType, c -> c));

        log.info("IngestService starting with {} connectors, poll interval = {}s",
                connectorMap.size(), pollIntervalSeconds);

        // Main polling loop
        Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(pollIntervalSeconds))
                .flatMap(tick -> processAllSubscriptions()
                        .onErrorResume(e -> {
                            log.error("Error in ingest cycle: {}", e.getMessage());
                            return Mono.empty();
                        })
                )
                .subscribe();

        log.info("IngestService started successfully");
    }

    private Mono<Void> processAllSubscriptions() {
        log.debug("Starting ingest cycle...");

        return subscriptionRepository.findByEnabled(true)
                .collectList()
                .flatMap(subscriptions -> {
                    log.debug("Processing {} active subscriptions", subscriptions.size());

                    // Group by sourceType + params for efficient polling
                    Map<String, List<Subscription>> grouped = subscriptions.stream()
                            .collect(Collectors.groupingBy(
                                    s -> s.getSourceType() + "::" + (s.getParams() != null ? s.getParams() : "")
                            ));

                    return Flux.fromIterable(grouped.entrySet())
                            .flatMap(entry -> {
                                List<Subscription> subs = entry.getValue();
                                Subscription representative = subs.get(0);

                                SourceConnector connector = connectorMap.get(representative.getSourceType());
                                if (connector == null) {
                                    log.warn("No connector for source type: {}", representative.getSourceType());
                                    return Flux.empty();
                                }

                                return connector.poll(representative.getParams() != null ? representative.getParams() : "{}")
                                        .flatMap(event -> processEvent(event, subs), 4)
                                        .onErrorResume(e -> {
                                            log.error("Error polling source {}: {}", representative.getSourceType(), e.getMessage());
                                            return Flux.empty();
                                        });
                            }, 4)
                            .then();
                });
    }

    private Mono<Void> processEvent(Event event, List<Subscription> subscriptions) {
        // Dedup by externalId
        return dedup(event)
                .flatMap(savedEvent ->
                        Flux.fromIterable(subscriptions)
                                .flatMap(sub -> processForSubscription(savedEvent, sub))
                                .then()
                )
                .onErrorResume(e -> {
                    log.error("Error processing event '{}': {}", event.getTitle(), e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Event> dedup(Event event) {
        if (event.getExternalId() == null || event.getExternalId().isBlank()) {
            return eventRepository.save(event);
        }

        return eventRepository.existsByExternalId(event.getExternalId())
                .flatMap(exists -> {
                    if (exists) {
                        log.debug("Event already exists: {}", event.getExternalId());
                        return Mono.empty();
                    }
                    return eventRepository.save(event);
                });
    }

    private Mono<Void> processForSubscription(Event event, Subscription subscription) {
        return ruleRepository.findBySubscriptionId(subscription.getId())
                .collectList()
                .flatMap(rules -> {
                    if (rules.isEmpty()) {
                        // No rules = notify with default priority
                        return createNotifications(subscription.getUserId(), event, event.getPriority());
                    }
                    return ruleEngine.evaluate(event, rules, subscription.getUserId())
                            .flatMap(priority -> createNotifications(subscription.getUserId(), event, priority));
                });
    }

    private Mono<Void> createNotifications(Long userId, Event event, String priority) {
        // Update event priority
        Event enriched = Event.builder()
                .id(event.getId())
                .sourceType(event.getSourceType())
                .externalId(event.getExternalId())
                .title(event.getTitle())
                .payloadJson(event.getPayloadJson())
                .priority(priority)
                .createdAt(event.getCreatedAt())
                .build();

        // Create UI notification (always)
        Mono<Notification> uiNotification = notificationService.createAndDeliver(userId, enriched, "UI");

        // Create external channel notifications
        Mono<Notification> telegramNotification = notificationService.createAndDeliver(userId, enriched, "TELEGRAM");

        return uiNotification
                .then(telegramNotification)
                .flatMap(notification -> deliveryService.deliver(notification, enriched))
                .onErrorResume(e -> {
                    log.error("Error creating notifications for user {}: {}", userId, e.getMessage());
                    return Mono.empty();
                });
    }
}
