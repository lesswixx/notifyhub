package com.notifyhub.ingest;

import com.notifyhub.model.Event;
import reactor.core.publisher.Flux;

/**
 * Interface for event source connectors.
 * Each connector knows how to poll/generate events from a specific source type.
 */
public interface SourceConnector {

    /**
     * @return the source type this connector handles (e.g., GITHUB, RSS, GEN)
     */
    String getSourceType();

    /**
     * Poll or generate events for the given subscription parameters.
     *
     * @param params JSON string with source-specific parameters
     * @return flux of normalized events
     */
    Flux<Event> poll(String params);
}
