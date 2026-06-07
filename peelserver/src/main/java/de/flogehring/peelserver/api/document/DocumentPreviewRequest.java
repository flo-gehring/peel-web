package de.flogehring.peelserver.api.document;

import de.flogehring.peelserver.api.PeelScriptId;

import java.util.Map;

public record DocumentPreviewRequest(
        Map<NameTagInDocument, PeelScriptId> scriptTags,
        Map<String, Object> bindings,
        String renderConfigId,
        String template
) {
}
