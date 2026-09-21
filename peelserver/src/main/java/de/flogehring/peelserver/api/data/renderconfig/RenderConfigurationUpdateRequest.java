package de.flogehring.peelserver.api.data.renderconfig;

public record RenderConfigurationUpdateRequest(
        String name,
        RenderConfigurationDto renderConfigurationDto
) {
}
