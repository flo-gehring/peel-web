package de.flogehring.peelserver.api.data.projects;

import de.flogehring.peelserver.api.data.ProjectIdApi;

public record ProjectSummary(
        ProjectIdApi projectIdApi,
        String name,
        String description,
        de.flogehring.peelserver.impl.project.ProjectState state
) {
}
