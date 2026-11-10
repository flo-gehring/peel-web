package de.flogehring.peelserver.api.data.template;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record DocumentTemplateSaveRequest(
        @NotNull @Valid ProjectIdApi projectId,
        String name,
        @NotNull Map<String, @NotBlank String> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        String renderConfigurationId
) {
}
