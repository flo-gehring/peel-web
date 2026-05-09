package de.flogehring.peelserver.api.document;

import java.time.Instant;
import java.util.Map;

public record DocumentContent(
        String id,
        Instant createdAt,
        Instant updatedAt
) {
}
