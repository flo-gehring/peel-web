package de.flogehring.peelserver.api.document;

import de.flogehring.peelserver.api.PeelScriptId;
import de.flogehring.peelserver.api.RenderConfigurationDto;

import java.util.Map;

public record DocumentPreviewRequest(
        Map<NameTagInDocument, PeelScriptId> scripTags,
        Map<String, Object> bindings,
        String renderConfigId,
        RenderConfigurationDto localOverrides,
        String template
) {
}
