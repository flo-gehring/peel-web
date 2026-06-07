package de.flogehring.peelserver.api;

import de.flogehring.peel.core.trace.TraceExpressionKind;

import java.util.Map;

public record RenderConfigurationDto(
        Map<TraceExpressionKind, String> renderConfigurations,
        Map<String, Map<TraceExpressionKind, String>> namedOverrides
) {
}
