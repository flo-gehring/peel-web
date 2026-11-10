package de.flogehring.peelserver.api.data.template;

import java.util.Map;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record DocumentTemplateUpdateRequest(
        String name,
        @NotNull Map<String, @NotBlank String> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        String renderConfigurationId
) {
}
