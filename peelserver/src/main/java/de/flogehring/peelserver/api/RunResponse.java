package de.flogehring.peelserver.api;

import java.util.List;
import java.util.Map;

public record RunResponse(List<Map<String, Object>> programmStatements, Map<String, Object> result) {
}
