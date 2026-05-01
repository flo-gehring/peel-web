package de.flogehring.peelserver.api;

import java.util.Map;

public record RunRequest(String script, Map<String, Object> bindings) {
}
