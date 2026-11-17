package de.flogehring.peelserver.api.data.scripts;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.List;
import de.flogehring.peelserver.impl.run.ExpectedBinding;
import jakarta.validation.Valid;

public record RunRequest(
        @jakarta.validation.constraints.NotNull @jakarta.validation.Valid de.flogehring.peelserver.api.data.ProjectIdApi projectId,
        String script,
        @jakarta.validation.constraints.NotNull List<@Valid ExpectedBinding> expectedBindings,
        @jakarta.validation.constraints.NotNull @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> bindings
) {
}
