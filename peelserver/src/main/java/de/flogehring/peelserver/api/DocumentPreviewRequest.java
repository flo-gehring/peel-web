package de.flogehring.peelserver.api;

import java.util.Map;

public record DocumentPreviewRequest(Map<String, Object> content, Map<String, Object> exampleBindings) {
}
