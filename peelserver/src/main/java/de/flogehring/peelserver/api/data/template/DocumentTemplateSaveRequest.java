package de.flogehring.peelserver.api.data.template;

import java.util.Map;

public record DocumentTemplateSaveRequest(
        String id,
        String name,
        Map<String, String> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        String renderConfigurationId
) {
}
