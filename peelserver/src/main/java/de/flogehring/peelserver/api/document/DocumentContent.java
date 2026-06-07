package de.flogehring.peelserver.api.document;

import java.time.Instant;

public record DocumentContent(
        String id,
        Instant createdAt,
        Instant updatedAt
) {
}
