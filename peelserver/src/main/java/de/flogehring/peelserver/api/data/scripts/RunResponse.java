package de.flogehring.peelserver.api.data.scripts;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record RunResponse(
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> trace,
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> result
) {
}
