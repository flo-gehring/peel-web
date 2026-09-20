package de.flogehring.peelserver.api.data.scripts;

public record ValidationDiagnostic(
        int line,
        int column,
        int endLine,
        int endColumn,
        String severity,
        String message
) {
}
