package de.flogehring.peelserver.api;

import java.time.Instant;
import java.util.Map;

public record DocumentResponse(
        String id,
        String name,
        String script,
        String template,
        Map<String, Object> exampleBindings,
        Instant createdAt,
        Instant updatedAt
) {
}
