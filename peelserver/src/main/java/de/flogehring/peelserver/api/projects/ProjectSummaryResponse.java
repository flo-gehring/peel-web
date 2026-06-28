package de.flogehring.peelserver.api.projects;

import java.time.Instant;

public record ProjectSummaryResponse(
        String id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        ProjectCounts counts,
        ProjectHealth health
) {
}
