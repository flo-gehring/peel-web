package de.flogehring.peelserver.api;

import java.util.Map;

public record TraceRenderRequest(
        String script,
        Map<String, Object> bindings,
        String template
) {
}
