package de.flogehring.peelserver.api.data.template;

import de.flogehring.peelserver.api.data.scripts.PeelScriptId;
import de.flogehring.peelserver.api.data.ProjectIdApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record DocumentPreviewRequest(
        @NotNull @Valid ProjectIdApi projectId,
        @NotNull Map<IdentifierInDocument, @NotNull @Valid PeelScriptId> scriptTags,
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> bindings,
        String renderConfigId,
        String template
) {
}
