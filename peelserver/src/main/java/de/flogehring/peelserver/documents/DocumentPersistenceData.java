package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.scripts.PeelScriptId;

import java.util.Map;

public record DocumentPersistenceData(
        String name,
        Map<String, PeelScriptId> scriptNameTags,
        String template,
        RenderConfigurationId renderConfigurationId,
        ExpressionRenderConfiguration localOverrides
) {
}
