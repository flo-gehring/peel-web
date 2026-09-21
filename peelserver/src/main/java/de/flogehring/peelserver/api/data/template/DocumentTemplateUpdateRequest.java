package de.flogehring.peelserver.api.data.template;

import java.util.Map;

public record DocumentTemplateUpdateRequest(
        String name,
        Map<String, String> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        String renderConfigurationId
) {
}
