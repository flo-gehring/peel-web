package de.flogehring.peelserver.api.document;

import de.flogehring.peelserver.api.RenderConfigurationDto;

import java.util.Map;

public record DocumentSaveRequest(
        String id,
        String name,
        Map<String, String> scriptNameTags,
        String template,
        String renderConfigurationId,
        RenderConfigurationDto localOverrides
) {
}
