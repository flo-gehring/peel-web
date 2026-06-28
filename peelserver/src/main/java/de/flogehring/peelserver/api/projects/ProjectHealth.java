package de.flogehring.peelserver.api.projects;

import java.util.List;

public record ProjectHealth(
        String status,
        List<String> issues
) {
}
