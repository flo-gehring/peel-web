package de.flogehring.peelserver.api;

import java.time.Instant;
import java.util.Map;

public record DocumentResponse(
        String id,
        String name,
        Map<String, Object> content,
        Map<String, Object> exampleBindings,
        Instant createdAt,
        Instant updatedAt
) {
}
