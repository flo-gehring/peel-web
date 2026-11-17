package de.flogehring.peelserver.api.data.scripts;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BlockDefinition(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String parentName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> reachableBlocks
) {
}
