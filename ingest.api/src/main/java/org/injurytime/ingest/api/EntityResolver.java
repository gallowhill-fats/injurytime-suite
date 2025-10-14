package org.injurytime.ingest.api;

/**
 *
 * @author clayton
 */
public interface EntityResolver {

    record Resolved(Long playerId, Long clubId, int adjustedConfidence) {}

    Resolved resolve(AvailabilityExtraction extraction);
}
