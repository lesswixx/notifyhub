package com.notifyhub.delivery;

import com.notifyhub.model.Event;
import com.notifyhub.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Email delivery channel.
 * Uses JavaMailSender (blocking) wrapped in boundedElastic scheduler
 * to maintain the reactive pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDeliveryChannel {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean enabled;

    @Value("${app.mail.from:noreply@notifyhub.com}")
    private String fromAddress;

    public Mono<Void> send(User user, Event event) {
        if (!enabled) {
            log.debug("Email delivery disabled, skipping");
            return Mono.empty();
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("User {} has no email", user.getUsername());
            return Mono.empty();
        }

        return Mono.fromCallable(() -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(fromAddress);
                    message.setTo(user.getEmail());
                    message.setSubject("[NotifyHub] " + event.getTitle());
                    message.setText(String.format(
                            "Event: %s\nSource: %s\nPriority: %s\n\nDetails:\n%s",
                            event.getTitle(),
                            event.getSourceType(),
                            event.getPriority(),
                            event.getPayloadJson() != null ? event.getPayloadJson() : "N/A"
                    ));
                    mailSender.send(message);
                    log.info("Email sent to {}", user.getEmail());
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
