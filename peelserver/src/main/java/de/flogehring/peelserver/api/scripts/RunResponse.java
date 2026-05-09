package de.flogehring.peelserver.api.scripts;

import java.util.Map;

public record RunResponse(Map<String, Object> trace, Map<String, Object> result) {
}
