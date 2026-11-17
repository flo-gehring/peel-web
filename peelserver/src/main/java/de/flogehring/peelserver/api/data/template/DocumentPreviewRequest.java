package de.flogehring.peelserver.api.data.template;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record DocumentPreviewRequest(
        @NotNull @Valid ProjectIdApi projectId,
        @NotNull DocumentNode document,
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> bindings,
        String renderConfigId
) {
}
