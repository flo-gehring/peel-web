package de.flogehring.peelserver.api;

import java.util.List;

public record ValidationResponse(List<ValidationDiagnostic> diagnostics) {
}
