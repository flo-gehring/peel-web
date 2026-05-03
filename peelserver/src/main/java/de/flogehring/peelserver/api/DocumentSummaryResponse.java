package de.flogehring.peelserver.api;

import java.time.Instant;

public record DocumentSummaryResponse(String id, String name, Instant updatedAt) {
}
