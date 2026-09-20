package de.flogehring.peelserver.api.data.template;

import de.flogehring.peelserver.api.data.scripts.PeelScriptId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record DocumentPreviewRequest(
        Map<IdentifierInDocument, PeelScriptId> scriptTags,
        @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> bindings,
        String renderConfigId,
        String template
) {
}
