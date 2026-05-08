package de.flogehring.peelserver.api;

import java.util.Map;

public record DocumentSaveRequest(
        String id,
        String name,
        String script,
        String template,
        Map<String, Object> exampleBindings
) {
}
