package de.flogehring.peelserver.api.data.renderconfig;

import de.flogehring.peelserver.api.data.ProjectIdApi;

public record RenderConfigurationPersistenceDto(
        String name,
        ProjectIdApi projectIdApi,
        RenderConfigurationDto renderConfigurationDto
) {
}
