package de.flogehring.peelserver.api.document;

import java.util.Map;

public record DocumentDto(
        String name,
        Map<String, String> scriptNameTags,
        String template,
        String templateHtml,
        String editorStateJson,
        String renderConfigId
) {
}
