package de.flogehring.peelserver.api.document;

import de.flogehring.peelserver.api.RenderConfigurationDto;

import java.util.Map;

public record DocumentDto(
        String name,
        Map<String, String> scriptNameTags,
        String template,
        String renderConfigId,
        RenderConfigurationDto localOverrides
) {
}
