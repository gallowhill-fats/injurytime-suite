package org.injurytime.ingest.api;


/**
 *
 * @author clayton
 */
public record RawItem(
        String sourceSystem,
        String sourceId,
        String sourceUri,
        String subject,
        String html,
        String text
        ) {

}
