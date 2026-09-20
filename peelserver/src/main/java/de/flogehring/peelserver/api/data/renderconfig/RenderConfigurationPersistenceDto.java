package de.flogehring.peelserver.api.data.renderconfig;

public record RenderConfigurationPersistenceDto(
        String name,
        RenderConfigurationDto renderConfigurationDto
) {
}
