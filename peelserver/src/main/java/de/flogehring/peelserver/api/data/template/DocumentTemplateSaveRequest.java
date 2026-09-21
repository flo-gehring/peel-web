package de.flogehring.peelserver.api.data.template;

import de.flogehring.peelserver.api.data.ProjectIdApi;

import java.util.Map;

public record DocumentTemplateSaveRequest(
        ProjectIdApi projectId,
        String name,
        Map<String, String> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        String renderConfigurationId
) {
}
