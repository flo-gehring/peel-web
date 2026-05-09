package de.flogehring.peelserver.api;

import jakarta.annotation.Nullable;

public record RenderConfigurationSaveRequest(
        @Nullable String id,
        RenderConfigurationDto renderConfigurationDto
) {
}
