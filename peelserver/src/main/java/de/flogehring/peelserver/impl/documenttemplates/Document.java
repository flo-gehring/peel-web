package de.flogehring.peelserver.impl.documenttemplates;

import de.flogehring.peelserver.impl.renderconfig.ExpressionRenderConfiguration;
import de.flogehring.peelserver.impl.scripts.PeelScript;

import java.util.Map;

public record Document(
        String name,
        Map<String, PeelScript> scriptNameTags,
        String template,
        ExpressionRenderConfiguration globalRenderConfiguration
) {
}
