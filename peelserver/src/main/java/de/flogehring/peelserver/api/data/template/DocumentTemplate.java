package de.flogehring.peelserver.api.data.template;

import java.time.Instant;
import java.util.Map;

public record DocumentTemplate(
        String id,
        String name,
        Map<String, String> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        String renderConfigurationId,
        Instant createdAt,
        Instant updatedAt
) {
}
