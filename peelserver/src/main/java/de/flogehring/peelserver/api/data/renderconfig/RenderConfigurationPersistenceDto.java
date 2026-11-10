package de.flogehring.peelserver.api.data.renderconfig;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RenderConfigurationPersistenceDto(
        String name,
        @NotNull @Valid ProjectIdApi projectIdApi,
        RenderConfigurationDto renderConfigurationDto
) {
}
