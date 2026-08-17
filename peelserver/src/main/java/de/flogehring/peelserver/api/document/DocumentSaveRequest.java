package de.flogehring.peelserver.api.document;

import java.util.Map;

public record DocumentSaveRequest(
        String id,
        String name,
        Map<String, String> scriptNameTags,
        String template,
        String renderConfigurationId
) {
}
