package de.flogehring.peelserver.api.scripts;

import java.util.Map;

public record ProjectRunRequest(String scriptId, String script, Map<String, Object> bindings) {
}
