package de.flogehring.peelserver.api.data.projects;

import de.flogehring.peelserver.api.data.ProjectIdApi;
import de.flogehring.peelserver.api.data.renderconfig.RenderConfigurationSummaryResponse;
import de.flogehring.peelserver.api.data.scripts.ScriptSummaryResponse;
import de.flogehring.peelserver.api.data.template.DocumentTemplateSummaryResponse;

import java.util.List;

public record ProjectData(
        ProjectIdApi projectIdApi,
        String name,
        de.flogehring.peelserver.impl.project.ProjectState state,
        List<ScriptSummaryResponse> scripts,
        List<DocumentTemplateSummaryResponse> documentTemplates,
        List<RenderConfigurationSummaryResponse> renderConfigurations
) {
}
