package de.flogehring.peelserver.api;

import java.util.Map;

public record DocumentSaveRequest(
        String id,
        String name,
        Map<String, Object> content,
        Map<String, Object> exampleBindings
) {
}
