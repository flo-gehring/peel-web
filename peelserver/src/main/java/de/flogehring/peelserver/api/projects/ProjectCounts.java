package de.flogehring.peelserver.api.projects;

public record ProjectCounts(
        int scripts,
        int documents,
        int renderConfigs
) {
}
