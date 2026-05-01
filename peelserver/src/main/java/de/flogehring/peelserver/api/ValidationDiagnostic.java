package de.flogehring.peelserver.api;

public record ValidationDiagnostic(
        int line,
        int column,
        int endLine,
        int endColumn,
        String severity,
        String message
) {
}
