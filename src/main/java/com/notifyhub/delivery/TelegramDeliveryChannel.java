package com.notifyhub.delivery;

import com.notifyhub.model.Event;
import com.notifyhub.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramDeliveryChannel {

    private final WebClient telegramWebClient;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.enabled:false}")
    private boolean enabled;

    public Mono<Void> send(User user, Event event) {
        if (!enabled) {
            log.debug("Telegram delivery disabled, skipping");
            return Mono.empty();
        }

        if (user.getTelegramChatId() == null || user.getTelegramChatId().isBlank()) {
            log.warn("User {} has no Telegram chat ID configured", user.getUsername());
            return Mono.empty();
        }

        String message = formatMessage(event);

        return telegramWebClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .bodyValue(new TelegramMessage(user.getTelegramChatId(), message, "HTML"))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(resp -> log.info("Telegram message sent to user {}", user.getUsername()))
                .then();
    }

    private String formatMessage(Event event) {
        return String.format(
                "<b>%s</b> [%s]\n\n%s\n\n<i>Source: %s | Priority: %s</i>",
                escapeHtml(event.getTitle()),
                event.getPriority(),
                truncate(event.getPayloadJson(), 500),
                event.getSourceType(),
                event.getPriority()
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    record TelegramMessage(String chat_id, String text, String parse_mode) {}
}
