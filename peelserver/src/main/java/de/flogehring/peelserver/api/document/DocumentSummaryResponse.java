package de.flogehring.peelserver.api.document;

import java.time.Instant;

public record DocumentSummaryResponse(String id, String name, Instant updatedAt) {
}
