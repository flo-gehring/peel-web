package de.flogehring.peelserver.api.data.template;

import java.time.Instant;

public record DocumentTemplateSummaryResponse(String id, String name, Instant updatedAt) {
}
