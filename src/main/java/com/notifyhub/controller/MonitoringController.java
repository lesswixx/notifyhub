package com.notifyhub.controller;

import com.notifyhub.repository.EventRepository;
import com.notifyhub.repository.NotificationRepository;
import com.notifyhub.repository.SubscriptionRepository;
import com.notifyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/stats")
    public Mono<Map<String, Object>> stats() {
        return Mono.zip(
                userRepository.count(),
                subscriptionRepository.count(),
                eventRepository.count(),
                notificationRepository.count()
        ).map(tuple -> Map.of(
                "totalUsers", (Object) tuple.getT1(),
                "totalSubscriptions", tuple.getT2(),
                "totalEvents", tuple.getT3(),
                "totalNotifications", tuple.getT4()
        ));
    }
}
