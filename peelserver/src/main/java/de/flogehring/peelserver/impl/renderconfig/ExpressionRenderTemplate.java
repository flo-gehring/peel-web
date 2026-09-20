package de.flogehring.peelserver.impl.renderconfig;

import de.flogehring.peel.core.trace.TraceExpressionKind;

import java.util.Map;

public record ExpressionRenderTemplate(Map<TraceExpressionKind, String> templates) {
}
