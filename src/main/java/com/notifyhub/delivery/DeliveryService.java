package com.notifyhub.delivery;

import com.notifyhub.model.Event;
import com.notifyhub.model.Notification;
import com.notifyhub.model.User;
import com.notifyhub.repository.UserRepository;
import com.notifyhub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Orchestrates delivery of notifications to external channels (Telegram, Email).
 * Uses retry with backoff for resilience.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final TelegramDeliveryChannel telegramChannel;
    private final EmailDeliveryChannel emailChannel;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Deliver a notification via the specified channel with retry logic.
     */
    public Mono<Void> deliver(Notification notification, Event event) {
        return userRepository.findById(notification.getUserId())
                .flatMap(user -> {
                    String channel = notification.getChannel();
                    return switch (channel) {
                        case "TELEGRAM" -> deliverTelegram(notification, event, user);
                        case "EMAIL" -> deliverEmail(notification, event, user);
                        default -> {
                            log.debug("No external delivery needed for channel: {}", channel);
                            yield Mono.empty();
                        }
                    };
                });
    }

    private Mono<Void> deliverTelegram(Notification notification, Event event, User user) {
        return notificationService.updateStatus(notification.getId(), "QUEUED", null)
                .then(telegramChannel.send(user, event))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(signal -> log.warn("Retrying Telegram delivery, attempt {}", signal.totalRetries() + 1)))
                .then(notificationService.updateStatus(notification.getId(), "SENT", null))
                .then()
                .onErrorResume(e -> {
                    log.error("Telegram delivery failed for notification {}: {}", notification.getId(), e.getMessage());
                    return notificationService.updateStatus(notification.getId(), "FAILED", e.getMessage()).then();
                });
    }

    private Mono<Void> deliverEmail(Notification notification, Event event, User user) {
        return notificationService.updateStatus(notification.getId(), "QUEUED", null)
                .then(emailChannel.send(user, event))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doBeforeRetry(signal -> log.warn("Retrying Email delivery, attempt {}", signal.totalRetries() + 1)))
                .then(notificationService.updateStatus(notification.getId(), "SENT", null))
                .then()
                .onErrorResume(e -> {
                    log.error("Email delivery failed for notification {}: {}", notification.getId(), e.getMessage());
                    return notificationService.updateStatus(notification.getId(), "FAILED", e.getMessage()).then();
                });
    }
}
