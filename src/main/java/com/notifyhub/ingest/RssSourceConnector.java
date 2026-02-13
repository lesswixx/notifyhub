package com.notifyhub.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.model.Event;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RssSourceConnector implements SourceConnector {

    private final WebClient generalWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getSourceType() {
        return "RSS";
    }

    @Override
    public Flux<Event> poll(String params) {
        try {
            JsonNode paramsNode = objectMapper.readTree(params);
            String url = paramsNode.has("url") ? paramsNode.get("url").asText() : null;

            if (url == null || url.isBlank()) {
                log.warn("RSS connector: no URL specified in params");
                return Flux.empty();
            }

            return generalWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMapMany(xml -> parseRssFeed(xml, url))
                    .onErrorResume(e -> {
                        log.error("RSS connector error for {}: {}", url, e.getMessage());
                        return Flux.empty();
                    });

        } catch (Exception e) {
            log.error("Failed to parse RSS connector params: {}", e.getMessage());
            return Flux.empty();
        }
    }

    private Flux<Event> parseRssFeed(String xml, String feedUrl) {
        return Mono.fromCallable(() -> {
                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feed = input.build(new StringReader(xml));
                    return feed.getEntries();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(entries -> Flux.fromIterable(entries))
                .take(20)
                .map(entry -> Event.builder()
                        .sourceType("RSS")
                        .externalId("rss:" + feedUrl + ":" + (entry.getUri() != null ? entry.getUri() : entry.getLink()))
                        .title(entry.getTitle())
                        .payloadJson(buildRssPayload(entry))
                        .priority("MEDIUM")
                        .createdAt(LocalDateTime.now())
                        .build()
                );
    }

    private String buildRssPayload(SyndEntry entry) {
        try {
            return objectMapper.writeValueAsString(new RssPayload(
                    entry.getTitle(),
                    entry.getLink(),
                    entry.getDescription() != null ? entry.getDescription().getValue() : null,
                    entry.getPublishedDate() != null ? entry.getPublishedDate().toString() : null
            ));
        } catch (Exception e) {
            return "{}";
        }
    }

    record RssPayload(String title, String link, String description, String publishedDate) {}
}
