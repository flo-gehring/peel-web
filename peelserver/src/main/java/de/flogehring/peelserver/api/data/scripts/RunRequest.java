package de.flogehring.peelserver.api.data.scripts;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record RunRequest(
        String script,
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> bindings
) {
}
