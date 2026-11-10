package de.flogehring.peelserver.api.data.scripts;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateScriptRequest(
        @NotNull @Valid ProjectIdApi projectIdApi,
        String name,
        String script
) {
}
