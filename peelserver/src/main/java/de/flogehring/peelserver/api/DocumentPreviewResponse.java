package de.flogehring.peelserver.api;

import java.util.List;
import java.util.Map;

public record DocumentPreviewResponse(
        Map<String, Object> renderedContent,
        List<DocumentPreviewReferenceStatus> references,
        List<DocumentPreviewDiagnostic> diagnostics
) {
}
