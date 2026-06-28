package de.flogehring.peelserver.api.projects;

public record CreateProjectRequest(
        String name,
        String id,
        String description,
        String template
) {
}
