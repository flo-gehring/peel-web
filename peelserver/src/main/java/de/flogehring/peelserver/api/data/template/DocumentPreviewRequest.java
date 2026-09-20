package de.flogehring.peelserver.api.data.template;

import de.flogehring.peelserver.api.data.scripts.PeelScriptId;

import java.util.Map;

public record DocumentPreviewRequest(
        Map<IdentifierInDocument, PeelScriptId> scriptTags,
        Map<String, Object> bindings,
        String renderConfigId,
        String template
) {
}
