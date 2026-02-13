package com.notifyhub.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubSourceConnector implements SourceConnector {

    private final WebClient githubWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public String getSourceType() {
        return "GITHUB";
    }

    @Override
    public Flux<Event> poll(String params) {
        try {
            JsonNode paramsNode = objectMapper.readTree(params);
            String repo = paramsNode.has("repo") ? paramsNode.get("repo").asText() : null;

            if (repo == null || repo.isBlank()) {
                log.warn("GitHub connector: no repo specified in params");
                return Flux.empty();
            }

            return githubWebClient.get()
                    .uri("/repos/{repo}/releases?per_page=10", repo)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .map(release -> Event.builder()
                            .sourceType("GITHUB")
                            .externalId("github:" + repo + ":" + release.path("id").asText())
                            .title(release.path("name").asText(release.path("tag_name").asText()))
                            .payloadJson(release.toString())
                            .priority("MEDIUM")
                            .createdAt(LocalDateTime.now())
                            .build()
                    )
                    .onErrorResume(e -> {
                        log.error("GitHub connector error for repo {}: {}", repo, e.getMessage());
                        return Flux.empty();
                    });

        } catch (Exception e) {
            log.error("Failed to parse GitHub connector params: {}", e.getMessage());
            return Flux.empty();
        }
    }
}
