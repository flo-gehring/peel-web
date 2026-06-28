package de.flogehring.peelserver.api.projects;

public record ProjectPaths(
        String root,
        String scriptsDir,
        String documentsDir,
        String renderConfigsDir,
        String fixturesDir,
        String outputDir
) {
}
