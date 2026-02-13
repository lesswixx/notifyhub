package com.notifyhub.ingest;

import com.notifyhub.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Internal event generator for load testing and demo purposes.
 * Generates synthetic events at configurable rate.
 */
@Slf4j
@Component
public class EventGeneratorConnector implements SourceConnector {

    private static final String[] TITLES = {
            "System Health Check Alert",
            "High CPU Usage Detected",
            "New Deployment Available",
            "Database Backup Completed",
            "Security Scan Finished",
            "Memory Usage Warning",
            "Service Restart Required",
            "SSL Certificate Expiring Soon",
            "New User Registration Spike",
            "API Rate Limit Approaching"
    };

    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH"};

    @Override
    public String getSourceType() {
        return "GEN";
    }

    @Override
    public Flux<Event> poll(String params) {
        // Generate 1-3 random events per poll
        int count = ThreadLocalRandom.current().nextInt(1, 4);

        return Flux.range(0, count)
                .map(i -> {
                    String title = TITLES[ThreadLocalRandom.current().nextInt(TITLES.length)];
                    String priority = PRIORITIES[ThreadLocalRandom.current().nextInt(PRIORITIES.length)];
                    String uniqueId = UUID.randomUUID().toString().substring(0, 8);

                    return Event.builder()
                            .sourceType("GEN")
                            .externalId("gen:" + uniqueId)
                            .title(title)
                            .payloadJson("{\"generated\":true,\"timestamp\":\"" + LocalDateTime.now() + "\",\"detail\":\"Auto-generated event #" + uniqueId + "\"}")
                            .priority(priority)
                            .createdAt(LocalDateTime.now())
                            .build();
                });
    }
}
