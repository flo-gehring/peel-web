package de.flogehring.peelserver.api.document;

import java.time.Instant;
import java.util.Map;

public record DocumentContent(
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
