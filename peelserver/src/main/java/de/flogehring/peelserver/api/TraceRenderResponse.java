package de.flogehring.peelserver.api;

import java.util.Map;

public record TraceRenderResponse(
        String html,
        Map<String, Object> trace,
        Map<String, Object> result
) {
}
