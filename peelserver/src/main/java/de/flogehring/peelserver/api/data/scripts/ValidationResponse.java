package de.flogehring.peelserver.api.data.scripts;

import java.util.List;

public record ValidationResponse(List<ValidationDiagnostic> diagnostics) {
}
