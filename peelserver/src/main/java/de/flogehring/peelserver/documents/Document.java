package de.flogehring.peelserver.documents;

import de.flogehring.peelserver.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.scripts.PeelScript;

import java.util.Map;

public record Document(
        String name,
        Map<String, PeelScript> scriptNameTags,
        String template,
        ExpressionRenderConfiguration globalRenderConfiguration,
        ExpressionRenderConfiguration localOverrides
) {
}
