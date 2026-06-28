package de.flogehring.peelserver.api.projects;

import java.time.Instant;

public record ProjectDetailResponse(
        String id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt,
        ProjectPaths paths,
        PeelProjectInfo peelProject,
        ProjectCounts counts,
        ProjectHealth health
) {
}
