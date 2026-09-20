package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.impl.renderconfig.RenderConfigurationId;
import de.flogehring.peelserver.impl.scripts.PeelScriptId;

import java.util.Map;

public record DocumentPersistenceData(
        String name,
        Map<String, PeelScriptId> scriptNameTags,
        String templateHtml,
        String editorStateJson,
        RenderConfigurationId renderConfigurationId
) {
}
