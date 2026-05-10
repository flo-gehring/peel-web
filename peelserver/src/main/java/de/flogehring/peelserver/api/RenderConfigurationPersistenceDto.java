package de.flogehring.peelserver.api;

public record RenderConfigurationPersistenceDto(
        String name,
        RenderConfigurationDto renderConfigurationDto
) {
}
