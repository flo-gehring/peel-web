package de.flogehring.peelserver.api.data.scripts;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.impl.run.ExpectedBinding;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AnalyzeRequest(
        @NotNull @Valid ProjectIdApi projectId,
        @NotNull String script,
        @NotNull List<@NotNull @Valid ExpectedBinding> expectedBindings
) {
}
